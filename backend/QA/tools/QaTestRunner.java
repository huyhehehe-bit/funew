import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** Entry point for running only QA tests when another member's sources do not compile. */
public final class QaTestRunner {
    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request().selectors(
                DiscoverySelectors.selectClass("com.funews.backend.AuthJwtIntegrationTest"),
                DiscoverySelectors.selectClass("com.funews.backend.AuthPasswordIntegrationTest"),
                DiscoverySelectors.selectClass("com.funews.backend.security.JwtTokenServiceTest"),
                DiscoverySelectors.selectClass("com.funews.backend.AccountFieldsServiceTest"),
                DiscoverySelectors.selectClass("com.funews.backend.RoleAccountFieldsPersistenceTest")).build();
        var listener = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        var summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));
        summary.printFailuresTo(new PrintWriter(System.out));
        if (summary.getTestsFoundCount() == 0 || summary.getTotalFailureCount() != 0
                || summary.getTestsSucceededCount() != summary.getTestsFoundCount()) {
            System.exit(1);
        }
    }
}
