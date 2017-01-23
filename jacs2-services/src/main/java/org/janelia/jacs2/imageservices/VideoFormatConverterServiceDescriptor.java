package org.janelia.jacs2.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.io.Files;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.model.service.ServiceMetaData;
import org.janelia.jacs2.service.impl.ServiceDescriptor;

import javax.inject.Inject;
import javax.inject.Named;

@Named("mpegConverter")
public class VideoFormatConverterServiceDescriptor implements ServiceDescriptor {
    private static String SERVICE_NAME = "fijiMacro";

    static class ConverterArgs {
        @Parameter(names = "-input", description = "Input file name", required = true)
        String input;
        @Parameter(names = "-output", description = "Output file name")
        String output;
        @Parameter(names = "-trunc", arity = 0, description = "Truncate flag", required = false)
        boolean truncate = false;

        String getOutputName() {
            if (StringUtils.isBlank(output)) {
                String extension = Files.getFileExtension(input);
                return input.replace(extension, ".mp4");
            }
            return output;
        }
    }

    private final VideoFormatConverterProcessor videoFormatConverterProcessor;

    @Inject
    VideoFormatConverterServiceDescriptor(VideoFormatConverterProcessor videoFormatConverterProcessor) {
        this.videoFormatConverterProcessor = videoFormatConverterProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        ServiceMetaData smd = new ServiceMetaData();
        smd.setServiceName(SERVICE_NAME);
        ConverterArgs args = new ConverterArgs();
        StringBuilder usageOutput = new StringBuilder();
        JCommander jc = new JCommander(args);
        jc.setProgramName(SERVICE_NAME);
        jc.usage(usageOutput);
        smd.setUsage(usageOutput.toString());
        return smd;
    }

    @Override
    public VideoFormatConverterProcessor createServiceProcessor() {
        return videoFormatConverterProcessor;
    }

}
