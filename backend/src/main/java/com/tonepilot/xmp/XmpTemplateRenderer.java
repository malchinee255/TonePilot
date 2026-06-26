package com.tonepilot.xmp;

import com.tonepilot.domain.ColorAdjustment;

import java.util.Map;

public class XmpTemplateRenderer {

    private final LightroomParamMapper mapper = new LightroomParamMapper();

    public String render(String presetName, ColorAdjustment adjustment) {
        StringBuilder attributes = new StringBuilder();
        for (Map.Entry<String, String> entry : mapper.toXmpAttributes(adjustment).entrySet()) {
            attributes.append("   ")
                    .append(entry.getKey())
                    .append("=\"")
                    .append(escape(entry.getValue()))
                    .append("\"\n");
        }

        return """
                <?xpacket begin="" id="W5M0MpCehiHzreSzNTczkc9d"?>
                <x:xmpmeta xmlns:x="adobe:ns:meta/">
                 <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                  <rdf:Description rdf:about=""
                   xmlns:crs="http://ns.adobe.com/camera-raw-settings/1.0/"
                   crs:PresetType="Normal"
                   crs:Cluster=""
                   crs:UUID="%s"
                   crs:SupportsAmount="False"
                   crs:SupportsColor="True"
                   crs:SupportsMonochrome="True"
                   crs:SupportsHighDynamicRange="True"
                   crs:SupportsNormalDynamicRange="True"
                   crs:SupportsSceneReferred="True"
                   crs:SupportsOutputReferred="True"
                   crs:CameraModelRestriction=""
                   crs:Copyright=""
                   crs:ContactInfo=""
                   crs:Version="15.0"
                   crs:ProcessVersion="11.0"
                   crs:Name="%s"
                %s  />
                 </rdf:RDF>
                </x:xmpmeta>
                <?xpacket end="w"?>
                """.formatted(java.util.UUID.randomUUID(), escape(presetName), attributes);
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
