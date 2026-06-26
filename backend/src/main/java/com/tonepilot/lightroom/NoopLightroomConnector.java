package com.tonepilot.lightroom;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NoopLightroomConnector implements LightroomConnector {

    @Override
    public LightroomConnectorStatus status() {
        return new LightroomConnectorStatus(
                false,
                "tonepilot-native-preview",
                "当前使用 TonePilot 原生预览渲染。若要真实驱动本地 Lightroom Classic，需要额外安装本地插件或 XMP 热文件夹桥接服务。",
                List.of(
                        "实现 Lightroom Classic Lua 插件，监听本地桥接请求并应用 Develop 参数",
                        "或使用 XMP 导出与热文件夹流程，让 Lightroom 导入并渲染结果",
                        "云端 Lightroom API 可作为 Creative Cloud catalog 集成，不等同于直接控制本机 Lightroom Classic"
                )
        );
    }
}
