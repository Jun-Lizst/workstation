package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceArg;
import org.janelia.jacs2.asyncservice.common.ServiceArgs;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.janelia.jacs2.asyncservice.common.ServiceExecutionContext;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Named("vaa3dPlugin")
public class Vaa3dPluginProcessor extends AbstractServiceProcessor<File> {

    static class Vaa3dPluginArgs extends ServiceArgs {
        @Parameter(names = {"-x", "-plugin"}, description = "Vaa3d plugin name", required = true)
        String plugin;
        @Parameter(names = {"-f", "-pluginFunc"}, description = "Vaa3d plugin function", required = true)
        String pluginFunc;
        @Parameter(names = {"-i", "-input"}, description = "Plugin input", required = true)
        String pluginInput;
        @Parameter(names = {"-o", "-output"}, description = "Plugin output", required = true)
        String pluginOutput;
        @Parameter(names = {"-p", "-pluginParams"}, description = "Plugin parameters")
        List<String> pluginParams = new ArrayList<>();
    }

    private final Vaa3dProcessor vaa3dProcessor;

    @Inject
    Vaa3dPluginProcessor(JacsServiceEngine jacsServiceEngine,
                         ServiceComputationFactory computationFactory,
                         JacsServiceDataPersistence jacsServiceDataPersistence,
                         @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                         Logger logger,
                         Vaa3dProcessor vaa3dProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.vaa3dProcessor = vaa3dProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Vaa3dPluginArgs());
    }

    @Override
    public File getResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToFile(jacsServiceData.getStringifiedResult());
    }

    @Override
    public void setResult(File result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.fileToString(result));
    }

    @Override
    protected ServiceComputation<JacsServiceData> preProcessData(JacsServiceData jacsServiceData) {
        try {
            Vaa3dPluginArgs args = getArgs(jacsServiceData);
            if (StringUtils.isBlank(args.pluginInput)) {
                return createFailure(jacsServiceData, new ComputationException(jacsServiceData, "Plugin input name must be specified"));
            } else if (StringUtils.isBlank(args.pluginOutput)) {
                return createFailure(jacsServiceData, new ComputationException(jacsServiceData, "Plugin output name must be specified"));
            } else {
                File pluginOutput = new File(args.pluginOutput);
                try {
                    Files.createDirectories(pluginOutput.getParentFile().toPath());
                } catch (IOException e) {
                    throw new ComputationException(jacsServiceData, e);
                }
                return createComputation(jacsServiceData);
            }
        } catch (Exception e) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, e));
        }
    }

    @Override
    protected List<JacsServiceData> submitAllDependencies(JacsServiceData jacsServiceData) {
        Vaa3dPluginArgs args = getArgs(jacsServiceData);
        return ImmutableList.of(submitVaa3dService(args, jacsServiceData));
    }

    @Override
    protected ServiceComputation<JacsServiceData> processData(JacsServiceData jacsServiceData) {
        return createComputation(jacsServiceData);
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        Vaa3dPluginArgs args = getArgs(jacsServiceData);
        return Files.exists(Paths.get(args.pluginOutput));
    }

    @Override
    protected File retrieveResult(JacsServiceData jacsServiceData) {
        Vaa3dPluginArgs args = getArgs(jacsServiceData);
        return new File(args.pluginOutput);
    }

    private JacsServiceData submitVaa3dService(Vaa3dPluginArgs args, JacsServiceData jacsServiceData) {
        StringJoiner vaa3Args = new StringJoiner(" ")
                .add("-x").add(args.plugin)
                .add("-f").add(args.pluginFunc)
                .add("-i").add(args.pluginInput)
                .add("-o").add(args.pluginOutput);
        if (CollectionUtils.isNotEmpty(args.pluginParams)) {
            vaa3Args.add(StringUtils.wrap(args.pluginParams.stream().collect(Collectors.joining(" ")), '"'));
        }
        return vaa3dProcessor.create(new ServiceExecutionContext(jacsServiceData),
                new ServiceArg("-vaa3dArgs", vaa3Args.toString()));
    }

    private Vaa3dPluginArgs getArgs(JacsServiceData jacsServiceData) {
        Vaa3dPluginArgs args = new Vaa3dPluginArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

}