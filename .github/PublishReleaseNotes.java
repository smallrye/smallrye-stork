///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.kohsuke:github-api:1.313
//DEPS info.picocli:picocli:4.7.0

import org.kohsuke.github.*;
import picocli.CommandLine;
import static java.lang.System.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Script computing the release notes for a specific release.
 *
 * Run with `./PublishReleaseNotes.java --token=GITHUB_TOKEN --release-version=version
 */
@CommandLine.Command(name = "publish", mixinStandardHelpOptions = true, version = "0.1",
        description = "Compute and publish release notes")
public class PublishReleaseNotes implements Callable<Integer> {

    @CommandLine.Option(names = "--token", description = "The Github Token", required = true)
    private String token;

    @CommandLine.Option(names = "--release-version", description = "Set the released version - if not set, the version is computed")
    private String target;

    
    public record Category(String name, String label) {    }

    private static Category MISC = new Category("⛮ Misc", "");

    private static List<Category> CATEGORIES = List.of(
        new Category("✨  New Features", "new-feature"),
        new Category("🫧  Enhancements", "enhancement"),
        new Category("📖  Documentation", "documentation"),
        new Category("🪲  Bug Fixes", "bug"),
        new Category("🔧  Dependency Upgrades", "dependencies"),
        MISC
    );

    private static final String REPO = "smallrye/smallrye-stork";
    

    public static void main(String... args) {
        int exitCode = new CommandLine(new PublishReleaseNotes()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        GitHub gitHub = new GitHubBuilder().withOAuthToken(token).build();
        GHRepository repository = gitHub.getRepository(REPO);

        var release = repository.getReleaseByTagName(target);
        if (release == null) {
            throw new IllegalAccessException("Unable to find the release " + target + " in " + REPO);
        }
        System.out.println("Found release " + target + " : " + release.getHtmlUrl());

        var milestone = getMilestone(repository);
        System.out.println("Found corresponding milestone " + target + " : " + milestone.getHtmlUrl());

        var issues = repository.getIssues(GHIssueState.CLOSED, milestone);
        System.out.println("Found " + issues.size() + " issues associated with the milestone " + target);


        Map<Category, List<GHIssue>> report = new HashMap<>();
        for (GHIssue issue : issues) {
            var category = getCategory(issue);
            report.computeIfAbsent(category, s -> new ArrayList<>()).add(issue);
        }

        var text = new StringBuffer();
        for(var category: CATEGORIES) {
            List<GHIssue> issueForCategory = report.get(category);
            if (issueForCategory!= null && ! issueForCategory.isEmpty()) {
                text.append("## ").append(category.name).append("\n\n");
                for (var i : issueForCategory) {
                    text.append("  * ").append(print(i)).append("\n");
                }
                text.append("\n");
            }
        }

        // Compatibility report
        File file = new File("target/differences.md");
        if (file.isFile()) {
            var compatibility = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            if (! compatibility.isBlank()) {
                text.append("## 🚨 Breaking Changes\n\n");
                text.append(compatibility).append("\n");
            }
        }
        

        System.out.println("Generated release notes: \n" + text);
        
        release.update().body(text.toString()).update();
        System.out.println("Release " + release.getHtmlUrl() + " updated.");
    
        return 0;
    }

    GHMilestone getMilestone(GHRepository repository) throws Exception {
        return repository.listMilestones(GHIssueState.CLOSED).toList().stream()
            .filter(m -> target.equalsIgnoreCase(m.getTitle()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Unable to find the milestone " + target));
    }

    Category getCategory(GHIssue issue) {
        var list = issue.getLabels().stream().map(l -> l.getName()).collect(Collectors.toList());
        if (list.isEmpty()) {
            return MISC;
        }
        for (String s : list) {
            for (var c : CATEGORIES) {
                if (c.label.equalsIgnoreCase(s)) {
                    return c;
                }
            }
        }
        System.out.println("No suitable category found for issue " + issue.getHtmlUrl() + " : " + list + " -> Using misc");
        return MISC;
    }

    String print(GHIssue issue) {
        var title = issue.getTitle();
        title = title.replace("build(deps):", "");
        if (title.contains("(deps-dev)")) {
            title = title.replace("build(deps-dev):", "");
            title = title + " (_test dependency_)";
        }
        title = title.trim();
        title = title.substring(0, 1).toUpperCase() + title.substring(1);
        return "[[#" + issue.getNumber() + "]](" + issue.getHtmlUrl() + ") - " + title;
    }

}
