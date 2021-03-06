package org.jenkinsci.plugins.github.pullrequest.dsl;

import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.tasks.Publisher;
import hudson.triggers.Trigger;
import hudson.util.DescribableList;
import javaposse.jobdsl.plugin.ExecuteDslScripts;
import javaposse.jobdsl.plugin.LookupStrategy;
import javaposse.jobdsl.plugin.RemovedJobAction;
import javaposse.jobdsl.plugin.RemovedViewAction;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.github.pullrequest.GitHubPRTrigger;
import org.jenkinsci.plugins.github.pullrequest.publishers.impl.GitHubPRBuildStatusPublisher;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.jenkinsci.plugins.github.pullrequest.GitHubPRTriggerMode.HEAVY_HOOKS;
import static org.junit.Assert.assertThat;

public class DslIntegrationTest {

    public static final String JOB_NAME_IN_DSL_SCRIPT = "gh-pull-request";
    public static final String JOB_DSL_GROOVY = "dsl/jobdsl.groovy";
    public static final String JOB_DSL_PUBLISHER_TEXT_CONTENT = "Build finished";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void shouldCreateJobWithExtendedDsl() throws Exception {
        FreeStyleProject job = jenkins.createFreeStyleProject();
        job.getBuildersList().add(
                new ExecuteDslScripts(
                        new ExecuteDslScripts.ScriptLocation(
                                null, null,
                                IOUtils.toString(this
                                        .getClass().getClassLoader().getResourceAsStream(JOB_DSL_GROOVY))
                        ),
                        false,
                        RemovedJobAction.DELETE,
                        RemovedViewAction.DELETE,
                        LookupStrategy.JENKINS_ROOT
                )
        );

        jenkins.buildAndAssertSuccess(job);

        assertThat(jenkins.getInstance().getJobNames(), hasItem(is(JOB_NAME_IN_DSL_SCRIPT)));

        FreeStyleProject generated = jenkins.getInstance()
                .getItemByFullName(JOB_NAME_IN_DSL_SCRIPT, FreeStyleProject.class);

        DescribableList<Publisher, Descriptor<Publisher>> publishers = generated.getPublishersList();

        assertThat("Should add publisher", publishers, hasSize(1));
        assertThat("Should add status publisher", publishers.get(0), instanceOf(GitHubPRBuildStatusPublisher.class));
        assertThat("Should add 2 packages",
                ((GitHubPRBuildStatusPublisher) publishers.get(0)).getStatusMsg().getContent(),
                equalTo(JOB_DSL_PUBLISHER_TEXT_CONTENT));

        Collection<Trigger<?>> triggers = generated.getTriggers().values();
        assertThat("Should add trigger", triggers, hasSize(1));
        Object trigger = triggers.toArray()[0];
        assertThat("Should add trigger of GHPR class", trigger, instanceOf(GitHubPRTrigger.class));
        assertThat("Should add events", ((GitHubPRTrigger) trigger).getEvents(), hasSize(2));
        assertThat("Should set mode", ((GitHubPRTrigger) trigger).getTriggerMode(), equalTo(HEAVY_HOOKS));
    }
}
