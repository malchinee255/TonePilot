local LrApplication = import "LrApplication"
local LrFileUtils = import "LrFileUtils"
local LrFunctionContext = import "LrFunctionContext"
local LrPathUtils = import "LrPathUtils"
local LrTasks = import "LrTasks"

local Config = dofile(LrPathUtils.child(_PLUGIN.path, "BridgeConfig.lua"))

local BridgeWorker = {
    running = false
}

local WORKER_BUILD = 18
local lastPreviewKey = ""
local lastPreviewAt = 0
local lastMetadataDebugKey = ""
local lastMetadataDebugAt = 0
local lastMetadataDebug = nil

function BridgeWorker.start()
    if BridgeWorker.running then
        return
    end
    BridgeWorker.running = true
    ensureDirectories()
    writeDiagnostic("worker-started.txt", "TonePilot Bridge Worker started at " .. tostring(os.time()))
    writeHeartbeat()
    LrTasks.startAsyncTask(function()
        LrFunctionContext.callWithContext("TonePilot Bridge Worker", function()
            writeDiagnostic("worker-task.txt", "TonePilot Bridge Worker LrTask started at " .. tostring(os.time()))
            while BridgeWorker.running do
                local ok, errorMessage = LrTasks.pcall(function()
                    writeHeartbeat()
                    writeSelectedPhotoState()
                    processApplyJobs()
                end)
                if not ok then
                    writeDiagnostic("plugin-error.txt", errorMessage)
                end
                LrTasks.sleep(Config.pollSeconds)
            end
        end)
    end, "TonePilot Bridge Worker")
end

function BridgeWorker.stop()
    BridgeWorker.running = false
end

function ensureDirectories()
    createDirectory(Config.bridgeRoot)
    createDirectory(path("apply-jobs"))
    createDirectory(path("processing"))
    createDirectory(path("results"))
    createDirectory(path("apply-results"))
end

function processApplyJobs()
    local files = LrFileUtils.files(path("apply-jobs"))
    for file in files do
        if string.match(file, "%.lua$") then
            processApplyJob(file)
        end
    end
end

function processApplyJob(jobPath)
    local processingPath = moveToProcessing(jobPath)
    local ok, jobOrError = pcall(function()
        local loader = assert(loadfile(processingPath))
        return loader()
    end)

    if not ok then
        writeApplyResultFromPath(processingPath, false, "读取 TonePilot 应用任务失败：" .. tostring(jobOrError))
        LrFileUtils.delete(processingPath)
        return
    end

    local job = jobOrError
    local success, message = LrTasks.pcall(function()
        applyDevelopSettingsJob(job)
    end)

    if success then
        writeApplyResult(job, true, "已应用到 Lightroom 当前照片。")
    else
        writeApplyResult(job, false, "Lightroom 应用参数失败：" .. tostring(message))
    end
    LrFileUtils.delete(processingPath)
end

function moveToProcessing(jobPath)
    local processingPath = LrPathUtils.child(path("processing"), LrPathUtils.leafName(jobPath))
    local moved = LrFileUtils.move(jobPath, processingPath)
    if moved == nil then
        return jobPath
    end
    return processingPath
end

function applyDevelopSettingsJob(job)
    local catalog = LrApplication.activeCatalog()
    local selected = currentTargetPhoto(catalog)
    if selected == nil then
        error("请先在 Lightroom Classic 中选中要修图的照片")
    end

    catalog:withWriteAccessDo("TonePilot Agent 应用调色参数", function()
        selected:applyDevelopSettings(job.developSettings or {})
    end, { timeout = Config.writeAccessTimeoutSeconds or 30 })

    local previewOk, previewUrlOrError = LrTasks.pcall(function()
        local previewFileName = job.previewFileName or (tostring(job.id or "agent-apply") .. ".jpg")
        local previewPath = job.previewPath or LrPathUtils.child(path("results"), previewFileName)
        exportPreview(selected, previewPath)
        return "/files/" .. previewFileName .. "?t=" .. tostring(os.time())
    end)
    if previewOk then
        job.previewUrl = previewUrlOrError
    else
        writeDiagnostic("apply-preview-error.txt", tostring(previewUrlOrError))
    end
end

function currentTargetPhoto(catalog)
    local selected = catalog:getTargetPhoto()
    if selected ~= nil then
        return selected
    end
    local ok, photos = pcall(function()
        return catalog:getTargetPhotos()
    end)
    if ok and photos ~= nil and #photos > 0 then
        return photos[1]
    end
    return nil
end

function writeSelectedPhotoState()
    local catalog = LrApplication.activeCatalog()
    local selected = nil
    local payload = nil

    catalog:withReadAccessDo(function()
        selected = currentTargetPhoto(catalog)
        if selected == nil then
            payload = {
                available = false,
                workerBuild = WORKER_BUILD,
                pluginPath = _PLUGIN.path,
                message = "Lightroom 当前没有选中照片",
                updatedAt = os.time()
            }
            return
        end

        payload = {
            available = true,
            workerBuild = WORKER_BUILD,
            pluginPath = _PLUGIN.path,
            updatedAt = os.time(),
            photo = photoMetadata(selected),
            currentAdjustment = currentAdjustmentFromPhoto(selected)
        }
        payload.metadataDebug = cachedMetadataDiagnostics(selected, payload.photo)
    end)

    if selected ~= nil and payload ~= nil then
        local previewOk, previewUrlOrError = LrTasks.pcall(function()
            return updateSelectedPreview(selected)
        end)
        if previewOk then
            payload.previewUrl = previewUrlOrError
        else
            payload.previewError = tostring(previewUrlOrError)
            writeDiagnostic("preview-error.txt", tostring(previewUrlOrError))
            if lastPreviewAt ~= 0 and LrFileUtils.exists(LrPathUtils.child(path("results"), "selected-preview.jpg")) then
                payload.previewUrl = "/files/selected-preview.jpg?t=" .. tostring(lastPreviewAt)
            end
        end
    end

    if payload ~= nil then
        writeFile(path("selected-photo.json"), encodeJson(payload))
        return
    end

    writeFile(path("selected-photo.json"), encodeJson({
        available = false,
        workerBuild = WORKER_BUILD,
        pluginPath = _PLUGIN.path,
        message = "无法读取 Lightroom 当前照片信息",
        updatedAt = os.time()
    }))
end

function updateSelectedPreview(photo)
    local fileName = firstNonEmpty(metadata(photo, "fileName"), metadata(photo, "path"))
    local copyName = metadata(photo, "copyName")
    local previewKey = fileName .. "::" .. copyName .. "::" .. developPreviewSignature(photo)
    local previewFileName = "selected-preview.jpg"
    local previewPath = LrPathUtils.child(path("results"), previewFileName)

    if previewKey ~= lastPreviewKey or not LrFileUtils.exists(previewPath) then
        exportPreview(photo, previewPath)
        lastPreviewKey = previewKey
        lastPreviewAt = os.time()
    end

    return "/files/" .. previewFileName .. "?t=" .. tostring(lastPreviewAt)
end

function cachedMetadataDiagnostics(photo, photoSummary)
    local now = os.time()
    local metadataKey = firstNonEmpty(photoSummary.path, photoSummary.fileName) .. "::" .. tostring(photoSummary.copyName or "")
    local interval = Config.metadataDebugIntervalSeconds or 30
    if lastMetadataDebug == nil or metadataKey ~= lastMetadataDebugKey or now - lastMetadataDebugAt >= interval then
        lastMetadataDebug = metadataDiagnostics(photo)
        lastMetadataDebugKey = metadataKey
        lastMetadataDebugAt = now
    end
    return lastMetadataDebug
end

function developPreviewSignature(photo)
    local settings = photo:getDevelopSettings()
    local keys = {
        "Exposure2012",
        "Contrast2012",
        "Highlights2012",
        "Shadows2012",
        "Whites2012",
        "Blacks2012",
        "Temperature",
        "Tint",
        "Texture",
        "Clarity2012",
        "Dehaze",
        "Vibrance",
        "Saturation",
        "HueAdjustmentRed",
        "SaturationAdjustmentRed",
        "LuminanceAdjustmentRed",
        "HueAdjustmentOrange",
        "SaturationAdjustmentOrange",
        "LuminanceAdjustmentOrange",
        "HueAdjustmentYellow",
        "SaturationAdjustmentYellow",
        "LuminanceAdjustmentYellow",
        "HueAdjustmentGreen",
        "SaturationAdjustmentGreen",
        "LuminanceAdjustmentGreen",
        "HueAdjustmentAqua",
        "SaturationAdjustmentAqua",
        "LuminanceAdjustmentAqua",
        "HueAdjustmentBlue",
        "SaturationAdjustmentBlue",
        "LuminanceAdjustmentBlue",
        "GrainAmount",
        "GrainSize",
        "GrainFrequency",
        "PostCropVignetteAmount",
        "PostCropVignetteMidpoint",
        "ParametricShadows",
        "ParametricDarks",
        "ParametricLights",
        "ParametricHighlights",
        "ParametricShadowSplit",
        "ParametricMidtoneSplit",
        "ParametricHighlightSplit",
        "Sharpness",
        "SharpenRadius",
        "SharpenDetail",
        "SharpenEdgeMasking",
        "LuminanceSmoothing",
        "ColorNoiseReduction",
        "ColorGradeShadowHue",
        "ColorGradeShadowSat",
        "ColorGradeMidtoneHue",
        "ColorGradeMidtoneSat",
        "ColorGradeHighlightHue",
        "ColorGradeHighlightSat",
        "ColorGradeBlending",
        "LensProfileEnable",
        "AutoLateralCA",
        "UprightTransformMode",
        "PerspectiveVertical",
        "PerspectiveHorizontal",
        "BluePrimaryHue",
        "BluePrimarySaturation"
    }
    local parts = {}
    for _, key in ipairs(keys) do
        table.insert(parts, key .. "=" .. tostring(settings[key] or ""))
    end
    return table.concat(parts, ";")
end

function metadataDiagnostics(photo)
    local keys = {
        "path",
        "fileName",
        "copyName",
        "fileFormat",
        "camera",
        "cameraMake",
        "cameraModel",
        "lens",
        "lensInfo",
        "isoSpeedRating",
        "iso",
        "focalLength",
        "shutterSpeed",
        "aperture",
        "dateTimeOriginal",
        "captureTime",
        "croppedDimensions",
        "dimensions"
    }
    local result = {}
    for _, key in ipairs(keys) do
        result[key] = metadataProbe(photo, key)
    end
    writeDiagnostic("metadata-debug.json", encodeJson(result))
    return result
end

function metadataProbe(photo, key)
    local formattedOk, formattedValue = LrTasks.pcall(function()
        return photo:getFormattedMetadata(key)
    end)
    local rawOk, rawValue = LrTasks.pcall(function()
        return photo:getRawMetadata(key)
    end)
    return {
        formattedOk = formattedOk,
        formattedValue = formattedOk and metadataValueToString(formattedValue) or "",
        formattedError = formattedOk and "" or tostring(formattedValue),
        rawOk = rawOk,
        rawValue = rawOk and metadataValueToString(rawValue) or "",
        rawError = rawOk and "" or tostring(rawValue)
    }
end

function photoMetadata(photo)
    local camera = firstNonEmpty(
        metadata(photo, "camera"),
        joinNonEmpty({ metadata(photo, "cameraMake"), metadata(photo, "cameraModel") }, " ")
    )

    return {
        path = metadata(photo, "path"),
        fileName = firstNonEmpty(metadata(photo, "fileName"), leafNameFromPath(metadata(photo, "path"))),
        copyName = metadata(photo, "copyName"),
        fileFormat = metadata(photo, "fileFormat"),
        camera = camera,
        lens = firstNonEmpty(metadata(photo, "lens"), metadata(photo, "lensInfo")),
        iso = firstNonEmpty(metadata(photo, "isoSpeedRating"), metadata(photo, "iso")),
        focalLength = metadata(photo, "focalLength"),
        shutterSpeed = metadata(photo, "shutterSpeed"),
        aperture = metadata(photo, "aperture"),
        captureTime = firstNonEmpty(metadata(photo, "dateTimeOriginal"), metadata(photo, "captureTime")),
        dimensions = firstNonEmpty(metadata(photo, "croppedDimensions"), metadata(photo, "dimensions"))
    }
end

function metadata(photo, key)
    local formattedOk, formattedValue = LrTasks.pcall(function()
        return photo:getFormattedMetadata(key)
    end)
    if formattedOk then
        local formatted = metadataValueToString(formattedValue)
        if formatted ~= "" then
            return formatted
        end
    end

    local rawOk, rawValue = LrTasks.pcall(function()
        return photo:getRawMetadata(key)
    end)
    if rawOk then
        return metadataValueToString(rawValue)
    end

    return ""
end

function metadataValueToString(value)
    if value == nil then
        return ""
    end
    if type(value) == "table" then
        return metadataTableToString(value)
    end
    local text = tostring(value)
    if text == "nil" then
        return ""
    end
    return text
end

function metadataTableToString(value)
    local width = value.width or value.w or value[1]
    local height = value.height or value.h or value[2]
    if width ~= nil and height ~= nil then
        return tostring(width) .. " x " .. tostring(height)
    end
    local parts = {}
    for _, item in ipairs(value) do
        local text = metadataValueToString(item)
        if text ~= "" then
            table.insert(parts, text)
        end
    end
    return table.concat(parts, " ")
end

function firstNonEmpty(...)
    for index = 1, select("#", ...) do
        local value = select(index, ...)
        if value ~= nil and tostring(value) ~= "" then
            return tostring(value)
        end
    end
    return ""
end

function joinNonEmpty(values, separator)
    local parts = {}
    for _, value in ipairs(values) do
        if value ~= nil and tostring(value) ~= "" then
            table.insert(parts, tostring(value))
        end
    end
    return table.concat(parts, separator)
end

function leafNameFromPath(filePath)
    if filePath == nil or filePath == "" then
        return ""
    end
    return LrPathUtils.leafName(filePath)
end

function currentAdjustmentFromPhoto(photo)
    local settings = photo:getDevelopSettings()
    return {
        style = "Lightroom 当前参数",
        reason = "由 Lightroom 插件后台读取当前选中照片参数",
        basic = {
            exposure = numberOrZero(settings.Exposure2012),
            contrast = intOrZero(settings.Contrast2012),
            highlights = intOrZero(settings.Highlights2012),
            shadows = intOrZero(settings.Shadows2012),
            whites = intOrZero(settings.Whites2012),
            blacks = intOrZero(settings.Blacks2012),
            temperature = intOrZero(settings.Temperature),
            tint = intOrZero(settings.Tint),
            texture = intOrZero(settings.Texture),
            clarity = intOrZero(settings.Clarity2012),
            dehaze = intOrZero(settings.Dehaze),
            vibrance = intOrZero(settings.Vibrance),
            saturation = intOrZero(settings.Saturation)
        },
        hsl = {
            redHue = intOrZero(settings.HueAdjustmentRed),
            redSaturation = intOrZero(settings.SaturationAdjustmentRed),
            redLuminance = intOrZero(settings.LuminanceAdjustmentRed),
            orangeHue = intOrZero(settings.HueAdjustmentOrange),
            orangeSaturation = intOrZero(settings.SaturationAdjustmentOrange),
            orangeLuminance = intOrZero(settings.LuminanceAdjustmentOrange),
            yellowHue = intOrZero(settings.HueAdjustmentYellow),
            yellowSaturation = intOrZero(settings.SaturationAdjustmentYellow),
            yellowLuminance = intOrZero(settings.LuminanceAdjustmentYellow),
            greenHue = intOrZero(settings.HueAdjustmentGreen),
            greenSaturation = intOrZero(settings.SaturationAdjustmentGreen),
            greenLuminance = intOrZero(settings.LuminanceAdjustmentGreen),
            aquaHue = intOrZero(settings.HueAdjustmentAqua),
            aquaSaturation = intOrZero(settings.SaturationAdjustmentAqua),
            aquaLuminance = intOrZero(settings.LuminanceAdjustmentAqua),
            blueHue = intOrZero(settings.HueAdjustmentBlue),
            blueSaturation = intOrZero(settings.SaturationAdjustmentBlue),
            blueLuminance = intOrZero(settings.LuminanceAdjustmentBlue),
            purpleHue = intOrZero(settings.HueAdjustmentPurple),
            purpleSaturation = intOrZero(settings.SaturationAdjustmentPurple),
            purpleLuminance = intOrZero(settings.LuminanceAdjustmentPurple),
            magentaHue = intOrZero(settings.HueAdjustmentMagenta),
            magentaSaturation = intOrZero(settings.SaturationAdjustmentMagenta),
            magentaLuminance = intOrZero(settings.LuminanceAdjustmentMagenta)
        },
        effects = {
            grain = intOrZero(settings.GrainAmount),
            vignette = intOrZero(settings.PostCropVignetteAmount)
        },
        extended = {
            processVersion = metadataValueToString(settings.ProcessVersion),
            treatment = metadataValueToString(settings.Treatment),
            profileName = metadataValueToString(settings.ProfileName),
            parametricShadows = intOrZero(settings.ParametricShadows),
            parametricDarks = intOrZero(settings.ParametricDarks),
            parametricLights = intOrZero(settings.ParametricLights),
            parametricHighlights = intOrZero(settings.ParametricHighlights),
            parametricShadowSplit = intOrZero(settings.ParametricShadowSplit),
            parametricMidtoneSplit = intOrZero(settings.ParametricMidtoneSplit),
            parametricHighlightSplit = intOrZero(settings.ParametricHighlightSplit),
            sharpness = intOrZero(settings.Sharpness),
            sharpenRadius = numberOrZero(settings.SharpenRadius),
            sharpenDetail = intOrZero(settings.SharpenDetail),
            sharpenEdgeMasking = intOrZero(settings.SharpenEdgeMasking),
            luminanceSmoothing = intOrZero(settings.LuminanceSmoothing),
            luminanceNoiseReductionDetail = intOrZero(settings.LuminanceNoiseReductionDetail),
            luminanceNoiseReductionContrast = intOrZero(settings.LuminanceNoiseReductionContrast),
            colorNoiseReduction = intOrZero(settings.ColorNoiseReduction),
            colorNoiseReductionDetail = intOrZero(settings.ColorNoiseReductionDetail),
            colorNoiseReductionSmoothness = intOrZero(settings.ColorNoiseReductionSmoothness),
            colorGradeShadowHue = intOrZero(settings.ColorGradeShadowHue),
            colorGradeShadowSat = intOrZero(settings.ColorGradeShadowSat),
            colorGradeMidtoneHue = intOrZero(settings.ColorGradeMidtoneHue),
            colorGradeMidtoneSat = intOrZero(settings.ColorGradeMidtoneSat),
            colorGradeHighlightHue = intOrZero(settings.ColorGradeHighlightHue),
            colorGradeHighlightSat = intOrZero(settings.ColorGradeHighlightSat),
            colorGradeBlending = intOrZero(settings.ColorGradeBlending),
            colorGradeGlobalHue = intOrZero(settings.ColorGradeGlobalHue),
            colorGradeGlobalSat = intOrZero(settings.ColorGradeGlobalSat),
            lensProfileEnable = intOrZero(settings.LensProfileEnable),
            lensManualDistortionAmount = intOrZero(settings.LensManualDistortionAmount),
            autoLateralCA = intOrZero(settings.AutoLateralCA),
            defringePurpleAmount = intOrZero(settings.DefringePurpleAmount),
            defringeGreenAmount = intOrZero(settings.DefringeGreenAmount),
            uprightTransformMode = intOrZero(settings.UprightTransformMode),
            perspectiveVertical = intOrZero(settings.PerspectiveVertical),
            perspectiveHorizontal = intOrZero(settings.PerspectiveHorizontal),
            perspectiveRotate = numberOrZero(settings.PerspectiveRotate),
            perspectiveScale = intOrZero(settings.PerspectiveScale),
            perspectiveAspect = intOrZero(settings.PerspectiveAspect),
            perspectiveX = intOrZero(settings.PerspectiveX),
            perspectiveY = intOrZero(settings.PerspectiveY),
            postCropVignetteStyle = intOrZero(settings.PostCropVignetteStyle),
            postCropVignetteMidpoint = intOrZero(settings.PostCropVignetteMidpoint),
            postCropVignetteRoundness = intOrZero(settings.PostCropVignetteRoundness),
            postCropVignetteFeather = intOrZero(settings.PostCropVignetteFeather),
            postCropVignetteHighlightContrast = intOrZero(settings.PostCropVignetteHighlightContrast),
            grainSize = intOrZero(settings.GrainSize),
            grainFrequency = intOrZero(settings.GrainFrequency),
            redPrimaryHue = intOrZero(settings.RedPrimaryHue),
            redPrimarySaturation = intOrZero(settings.RedPrimarySaturation),
            greenPrimaryHue = intOrZero(settings.GreenPrimaryHue),
            greenPrimarySaturation = intOrZero(settings.GreenPrimarySaturation),
            bluePrimaryHue = intOrZero(settings.BluePrimaryHue),
            bluePrimarySaturation = intOrZero(settings.BluePrimarySaturation),
            shadowTint = intOrZero(settings.ShadowTint),
            cropTop = numberOrZero(settings.CropTop),
            cropLeft = numberOrZero(settings.CropLeft),
            cropBottom = numberOrZero(settings.CropBottom),
            cropRight = numberOrZero(settings.CropRight),
            cropAngle = numberOrZero(settings.CropAngle),
            orientation = intOrZero(settings.Orientation)
        }
    }
end

function exportPreview(photo, previewPath)
    local previewDir = LrPathUtils.parent(previewPath)
    createDirectory(previewDir)

    local jpegData = nil
    local done = false
    photo:requestJpegThumbnail(1200, 1200, function(jpeg)
        jpegData = jpeg
        done = true
    end)

    local startedAt = os.time()
    while not done do
        if os.time() - startedAt > 15 then
            error("Lightroom 生成预览缩略图超时")
        end
        LrTasks.sleep(0.1)
    end

    if jpegData == nil or jpegData == "" then
        error("Lightroom 没有生成预览缩略图")
    end
    writeBinaryFile(previewPath, jpegData)
end

function writeHeartbeat()
    writeFile(path("heartbeat.txt"), tostring(os.time()))
end

function writeDiagnostic(name, content)
    writeFile(path(name), tostring(content))
end

function writeApplyResult(job, success, message)
    local content = "success=" .. tostring(success) .. "\nmessage=" .. sanitizeLine(message) .. "\n"
    if job.previewUrl ~= nil and tostring(job.previewUrl) ~= "" then
        content = content .. "previewUrl=" .. sanitizeLine(job.previewUrl) .. "\n"
    end
    writeFile(job.resultPath, content)
end

function writeApplyResultFromPath(jobPath, success, message)
    local name = LrPathUtils.removeExtension(LrPathUtils.leafName(jobPath))
    local resultPath = LrPathUtils.child(path("apply-results"), name .. ".result")
    local content = "success=" .. tostring(success) .. "\nmessage=" .. sanitizeLine(message) .. "\n"
    writeFile(resultPath, content)
end

function writeFile(filePath, content)
    local file = assert(io.open(filePath, "w"))
    file:write(content)
    file:close()
end

function writeBinaryFile(filePath, content)
    local file = assert(io.open(filePath, "wb"))
    file:write(content)
    file:close()
end

function createDirectory(directory)
    if not LrFileUtils.exists(directory) then
        LrFileUtils.createDirectory(directory)
    end
end

function path(child)
    return LrPathUtils.child(Config.bridgeRoot, child)
end

function numberOrZero(value)
    local number = tonumber(value)
    if number == nil then
        return 0
    end
    return number
end

function intOrZero(value)
    return math.floor(numberOrZero(value) + 0.5)
end

function sanitizeLine(value)
    return tostring(value):gsub("[\r\n]", " ")
end

function encodeJson(value)
    local valueType = type(value)
    if valueType == "table" then
        local parts = {}
        local isArray = isArrayTable(value)
        if isArray then
            for index = 1, #value do
                table.insert(parts, encodeJson(value[index]))
            end
            return "[" .. table.concat(parts, ",") .. "]"
        end
        for key, item in pairs(value) do
            table.insert(parts, encodeJson(tostring(key)) .. ":" .. encodeJson(item))
        end
        return "{" .. table.concat(parts, ",") .. "}"
    elseif valueType == "string" then
        return "\"" .. escapeJson(value) .. "\""
    elseif valueType == "number" or valueType == "boolean" then
        return tostring(value)
    end
    return "null"
end

function isArrayTable(value)
    local count = 0
    for key, _ in pairs(value) do
        if type(key) ~= "number" then
            return false
        end
        count = count + 1
    end
    return count == #value
end

function escapeJson(value)
    value = string.gsub(value, "\\", "\\\\")
    value = string.gsub(value, "\"", "\\\"")
    value = string.gsub(value, "\n", "\\n")
    value = string.gsub(value, "\r", "\\r")
    value = string.gsub(value, "\t", "\\t")
    return value
end

return BridgeWorker
