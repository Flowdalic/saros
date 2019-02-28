package de.fu_berlin.inf.dpp.git;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Set;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.RemoteRemoveCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.NullProgressMonitor;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.BundleWriter;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.TransportBundleStream;
import org.eclipse.jgit.transport.URIish;

public class JGitFacade {
  private static final Logger log = Logger.getLogger(JGitFacade.class);

  /**
   * Create a {@code bundle} with all commits from {@code basis} to {@code actual} of the Git repo.
   *
   * @param workDirTree The directory that contains the .git directory
   * @param actual The name of the ref to lookup. For example: "HEAD" or "refs/heads/master". Must
   *     not be a short-handform; e.g., "master" is not automatically expanded
   *     to"refs/heads/master". Include the object the ref is point at in the {@code bundle} and (if
   *     the {@code basis} is an empty string) everything reachable from it.
   * @param basis Assume that the recipient have at least the commit the {@code basis} is pointing
   *     to. In order to fetch from a {@code bundle} the recipient must have the commit the {@code
   *     basis} is pointing to.
   * @throws IllegalArgumentException {@code workDirTree} is null or if resolving {@code actual}
   *     leads to a not existing ref
   * @throws IOException failed while read/resolved parameters or while written to the {@code
   *     bundle}
   */
  public static byte[] createBundle(File workDirTree, String actual, String basis)
      throws IllegalArgumentException, IOException {

    Git git = getGitByworkDirTree(workDirTree);
    Repository repo = git.getRepository();
    // In order to create the bundle object we need to initialize the Bundlewriter and set it up
    // with the commit, it should end with and the start commit.
    BundleWriter bundlewriter = new BundleWriter(repo);

    // The first information the bundlewriter need is the commit to end with. (I choose the name of
    // the parameter because the end commit string is expected to be the actual position.)
    Ref actualRef;
    try {
      actualRef = repo.exactRef(actual);
    } catch (IOException e) {
      throw new IOException("failed while resolved actual", e);
    }
    if (actualRef == null)
      throw new IllegalArgumentException("failed while resolved actual: not existing ref");

    bundlewriter.include("refs/heads/bundle", actualRef.getObjectId());

    // The second information is the commit to start with
    if (basis != null && !basis.trim().isEmpty()) {
      try {
        RevWalk walk = new RevWalk(repo);

        RevCommit basisCommit = walk.parseCommit(repo.resolve(basis));

        walk.close();

        bundlewriter.assume(basisCommit);
      } catch (NullPointerException | IOException e) {
        throw new IOException("failed while resolved basis", e);
      }
    }

    // After finding the range of commits in the previous steps the bundle object will be written.
    NullProgressMonitor monitor = NullProgressMonitor.INSTANCE;
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    bundlewriter.writeBundle(monitor, byteArrayOutputStream);

    return byteArrayOutputStream.toByteArray();
  }

  /**
   * Fetching from a bundle to a Git repo
   *
   * @param workDirTree The directory that contains the .git directory
   * @param bundle See <a href="https://git-scm.com/docs/git-bundle">git bundle</a>
   * @throws IllegalArgumentException {@code workDirTree} or {@code bundle} is null and can't be
   *     resolved
   * @throws IOException failed while read from {@code workDirTree}, {@code bundle} handling or
   *     while fetch
   * @throws URISyntaxException
   */
  public static void fetchFromBundle(File workDirTree, byte[] bundle)
      throws IllegalArgumentException, IOException, URISyntaxException {

    Git git = getGitByworkDirTree(workDirTree);

    if (bundle == null) throw new IllegalArgumentException("bundle is null and can't be resolved");

    try {
      RemoteRemoveCommand remoteRemoveCommand = git.remoteRemove();
      remoteRemoveCommand.setName("bundle");
      remoteRemoveCommand.call();
    } catch (GitAPIException e) {
      throw new IOException("failed to remove old bundle path", e);
    }

    final URIish uri = new URIish("in-memory://");
    final ByteArrayInputStream in = new ByteArrayInputStream(bundle);
    final RefSpec rs = new RefSpec("refs/heads/*:refs/heads/*");
    final Set<RefSpec> refs = Collections.singleton(rs);

    try (TransportBundleStream transport =
        new TransportBundleStream(git.getRepository(), uri, in)) {
      transport.fetch(NullProgressMonitor.INSTANCE, refs);
    } catch (org.eclipse.jgit.errors.TransportException e) {
      throw new IOException("fetch failed", e);
    }
  }

  /**
   * @param workDirTree The directory that contains the .git directory
   * @return The URL to access/address directly the .git directory
   * @throws IllegalArgumentException {@code workDirTree} is null can't be resolved
   * @throws IOException failed while read
   */
  static String getUrlByworkDirTree(File workDirTree) throws IllegalArgumentException, IOException {
    Git git = getGitByworkDirTree(workDirTree);
    return git.getRepository().getDirectory().getCanonicalPath();
  }

  /**
   * @param workDirTree The directory that contains the .git directory
   * @param revString See <a href="https://www.git-scm.com/docs/gitrevisions">gitrevisions</a>
   * @throws IllegalArgumentException {@code workDirTree} is null can't be resolved
   * @throws IOException failed while read
   */
  public static String getSHA1HashByRevisionString(File workDirTree, String revString)
      throws IllegalArgumentException, IOException {
    Git git = getGitByworkDirTree(workDirTree);
    return git.getRepository().resolve(revString).name();
  }
  /** @param workDirTree The directory that contains the .git directory */
  private static Git getGitByworkDirTree(File workDirTree) throws IOException {
    try {
      if (workDirTree == null)
        throw new IllegalArgumentException("workDirTree is null and can't be resolved");
      return Git.open(workDirTree);
    } catch (IOException e) {
      throw new IOException("failed while read from workDirTree", e);
    }
  }

  /**
   * Allowing to merge commits if fast forward merge is possible. The current commit has to be a
   * ancestor of the named commit (which is accessed by resolving the given Revision String).
   *
   * @param workDirTree The directory that contains the .git directory
   * @param revString See <a href="https://www.git-scm.com/docs/gitrevisions">gitrevisions</a>
   * @throws IOException failed while merge
   */
  public static void ffMerge(File workDirTree, String revString) throws IOException {
    try {
      Git git = getGitByworkDirTree(workDirTree);
      git.merge()
          .include(git.getRepository().resolve(revString))
          .setFastForward(FastForwardMode.FF_ONLY)
          .call();
    } catch (IOException | RevisionSyntaxException | GitAPIException e) {
      throw new IOException("merge failed", e);
    }
  }
}
