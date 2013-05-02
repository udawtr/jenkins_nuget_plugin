package hudson.plugins.nuget;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * @author Wataru Uda
 */
public final class NuGetInstallation extends ToolInstallation implements NodeSpecific<NuGetInstallation>, EnvironmentSpecific<NuGetInstallation> {

    @SuppressWarnings("unused")
    /**
     * Backward compatibility
     */
    private transient String pathToNuGet;

    private String defaultArgs;

    @DataBoundConstructor
    public NuGetInstallation(String name, String home, String defaultArgs) {
        super(name, home, null);
        this.defaultArgs = Util.fixEmpty(defaultArgs);
    }

    public NuGetInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new NuGetInstallation(getName(), translateFor(node, log), getDefaultArgs());
    }

    public NuGetInstallation forEnvironment(EnvVars environment) {
        return new NuGetInstallation(getName(), environment.expand(getHome()), getDefaultArgs());
    }

    public String getDefaultArgs() {
        return this.defaultArgs;
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<NuGetInstallation> {

        public String getDisplayName() {
            return "NuGet";
        }

        @Override
        public NuGetInstallation[] getInstallations() {
            return Hudson.getInstance().getDescriptorByType(NuGetBuilder.DescriptorImpl.class).getInstallations();
        }

        @Override
        public void setInstallations(NuGetInstallation... installations) {
            Hudson.getInstance().getDescriptorByType(NuGetBuilder.DescriptorImpl.class).setInstallations(installations);
        }

    }

    /**
     * Used for backward compatibility
     *
     * @return the new object, an instance of NuGetnstallation
     */
    protected Object readResolve() {
        if (this.pathToNuGet != null) {
            return new NuGetInstallation(this.getName(), this.pathToNuGet, this.defaultArgs);
        }
        return this;
    }
}
