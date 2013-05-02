package hudson.plugins.nuget;

import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.tools.ToolInstallation;
import hudson.util.ArgumentListBuilder;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Map;

/**
 * @author udawtr@gmail.com
 */
public class NuGetBuilder extends Builder {
    /**
     * GUI fields
     */
    private final String nuGetName;
    private final String nuGetCommand;
    private final String nuGetFile;
    private final String cmdLineArgs;

    /**
     * When this builder is created in the project configuration step,
     * the builder object will be created from the strings below.
     *
     * @param nuGetName                The NuGet logical name
     * @param nuGetFile                The name/location of the package or project file
     * @param nuGetCommand             The command: update or install
     * @param cmdLineArgs                Whitespace separated list of command line arguments
     */
    @DataBoundConstructor
    @SuppressWarnings("unused")
    public NuGetBuilder(String nuGetName, String nuGetCommand, String nuGetFile, String cmdLineArgs) {
        this.nuGetName = nuGetName;
        this.nuGetCommand = nuGetCommand;
        this.nuGetFile = nuGetFile;
        this.cmdLineArgs = cmdLineArgs;
    }

    @SuppressWarnings("unused")
    public String getNuGetFile() {
        return nuGetFile;
    }

    @SuppressWarnings("unused")
    public String getNuGetCommand() {
        return nuGetCommand;
    }

    @SuppressWarnings("unused")
    public String getNuGetName() {
        return nuGetName;
    }

    @SuppressWarnings("unused")
    public String getCmdLineArgs() {
        return cmdLineArgs;
    }

    public NuGetInstallation getNuGet() {
        for (NuGetInstallation i : DESCRIPTOR.getInstallations()) {
            if (nuGetName != null && i.getName().equals(nuGetName))
                return i;
        }

        return null;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        ArgumentListBuilder args = new ArgumentListBuilder();
        String execName = "nuget.exe";
        NuGetInstallation ai = getNuGet();

        if (ai == null) {
            listener.getLogger().println("Path To NuGet.exe: " + execName);
            args.add(execName);
        } else {
            EnvVars env = build.getEnvironment(listener);
            ai = ai.forNode(Computer.currentComputer().getNode(), listener);
            ai = ai.forEnvironment(env);
            String pathToNuGet = ai.getHome();
            FilePath exec = new FilePath(launcher.getChannel(), pathToNuGet);

            try {
                if (!exec.exists()) {
                    listener.fatalError(pathToNuGet + " doesn't exist");
                    return false;
                }
            } catch (IOException e) {
                listener.fatalError("Failed checking for existence of " + pathToNuGet);
                return false;
            }

            listener.getLogger().println("Path To NuGet.exe: " + pathToNuGet);
            args.add(pathToNuGet);
        }
        
        //Command: install or update
        args.add(this.nuGetCommand);
        
        //If a target file is specified, then add it as an argument, otherwise
        //nuget will search for packages.config
        EnvVars env = build.getEnvironment(listener);
        String normalizedFile = null;
        if (nuGetFile != null && nuGetFile.trim().length() != 0) {
            normalizedFile = nuGetFile.replaceAll("[\t\r\n]+", " ");
            normalizedFile = Util.replaceMacro(normalizedFile, env);
            normalizedFile = Util.replaceMacro(normalizedFile, build.getBuildVariables());
            if (normalizedFile.length() > 0) {
                args.add(normalizedFile);
            }
        }

        String normalizedArgs = cmdLineArgs.replaceAll("[\t\r\n]+", " ");
        normalizedArgs = Util.replaceMacro(normalizedArgs, env);
        normalizedArgs = Util.replaceMacro(normalizedArgs, build.getBuildVariables());

        if (normalizedArgs.trim().length() > 0)
            args.addTokenized(normalizedArgs);
        
        //Default args
        if (ai.getDefaultArgs() != null) {
            args.addTokenized(ai.getDefaultArgs());
        }

        FilePath pwd = build.getModuleRoot();
        if (normalizedFile != null) {
            FilePath nuGetFilePath = pwd.child(normalizedFile);
            if (!nuGetFilePath.exists()) {
                pwd = build.getWorkspace();
            }
        }

        if (!launcher.isUnix()) {
            args.prepend("cmd.exe", "/C");
            args.add("&&", "exit", "%%ERRORLEVEL%%");
        }

        try {
            listener.getLogger().println(String.format("Executing the command %s from %s", args.toStringWithQuote(), pwd));
            int r = launcher.launch().cmds(args).envs(env).stdout(listener).pwd(pwd).join();
            return (r == 0);
        } catch (IOException e) {
            Util.displayIOException(e, listener);
            build.setResult(Result.FAILURE);
            return false;
        }
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Descriptor should be singleton.
     */
    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
    
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        @CopyOnWrite
        private volatile NuGetInstallation[] installations = new NuGetInstallation[0];

        DescriptorImpl() {
            super(NuGetBuilder.class);
            load();
        }

        public String getDisplayName() {
            return Messages.DisplayName();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        public NuGetInstallation[] getInstallations() {
            return installations;
        }

        public void setInstallations(NuGetInstallation... antInstallations) {
            this.installations = antInstallations;
            save();
        }

        public NuGetInstallation.DescriptorImpl getToolDescriptor() {
            return ToolInstallation.all().get(NuGetInstallation.DescriptorImpl.class);
        }
    }
}
