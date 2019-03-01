package de.fu_berlin.inf.dpp.git;

import de.fu_berlin.inf.dpp.activities.GitCollectActivity;
import de.fu_berlin.inf.dpp.activities.GitRequestActivity;
import de.fu_berlin.inf.dpp.activities.GitSendBundleActivity;
import de.fu_berlin.inf.dpp.annotations.Component;
import de.fu_berlin.inf.dpp.session.AbstractActivityConsumer;
import de.fu_berlin.inf.dpp.session.AbstractActivityProducer;
import de.fu_berlin.inf.dpp.session.IActivityConsumer;
import de.fu_berlin.inf.dpp.session.IActivityConsumer.Priority;
import de.fu_berlin.inf.dpp.session.internal.SarosSession;
import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.picocontainer.Startable;

/**
 * This Class is responsible for the negotiation between user that want to sent,receive and fetch
 * commits.
 *
 * <p>In order to start the negotiation the user needs to call the procedure {@code
 * sendCommitRequest()}.
 *
 * <p>Before starting negotiation the user need to set up his working directory tree by calling the
 * procedure {@code changeWorkDirTree(File workDirTree)}. After calling it the first time the
 * Manager hold a {@link JGitFacade} object.
 */
@Component(module = "core")
public class GitManager extends AbstractActivityProducer implements Startable {

  private static final Logger log = Logger.getLogger(GitManager.class.getName());

  private final SarosSession session;

  JGitFacade jGitFacade;

  public GitManager(SarosSession session) {
    this.session = session;
  }

  @Override
  public void start() {
    session.addActivityProducer(this);
    session.addActivityConsumer(consumer, Priority.ACTIVE);
  }

  @Override
  public void stop() {
    session.removeActivityProducer(this);
    session.removeActivityConsumer(consumer);
  }

  public void sendCommitRequest() {
    fireActivity(new GitRequestActivity(session.getLocalUser()));
  }

  public void changeWorkDirTree(File workDirTree) {
    log.info("You changed your working directory tree");
    if (jGitFacade == null) {
      this.jGitFacade = new JGitFacade(workDirTree);
    } else {
      this.jGitFacade.setWorkDirTree(workDirTree);
    }
  }

  private final IActivityConsumer consumer =
      new AbstractActivityConsumer() {
        @Override
        public void receive(GitRequestActivity activity) {
          if (jGitFacade != null) {
            log.info("You recieved a request to fetch commits");
            try {

              String basis = jGitFacade.getSHA1HashByRevisionString("HEAD");
              fireActivity(new GitCollectActivity(session.getLocalUser(), basis));

            } catch (IOException e) {
              log.debug(session.getLocalUser().toString() + " can't access the Git Directory");
            }
          } else {
            log.info("Your workDirTree is not selected.");
          }
        }

        @Override
        public void receive(GitCollectActivity activity) {
          if (jGitFacade != null) {
            log.info("Your request to send your commits was accepted.");
            String basis = activity.getBasis();
            try {
              byte[] bundle = jGitFacade.createBundle("HEAD", basis);
              fireActivity(new GitSendBundleActivity(session.getLocalUser(), bundle));

            } catch (IOException e) {
              log.info("Please check that the recievers HEAD is ancestor of your HEAD.");
              log.debug("failed at create bundle");
            } catch (IllegalArgumentException e) {
              log.debug(e);
            }
          } else {
            log.info("Your workDirTree is not selected.");
          }
        }

        @Override
        public void receive(GitSendBundleActivity activity) {
          if (jGitFacade != null) {
            log.info("You recieving the commits between your HEAD and the senders HEAD");
            byte[] bundle = activity.getBundle();
            try {
              jGitFacade.fetchFromBundle(bundle);
              log.info("Fetched the commits");
            } catch (Exception e) {
              log.debug(e);
            }
          } else {
            log.info("Your workDirTree is not selected.");
          }
        }
      };
}
