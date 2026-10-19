package org.opencivic.signalos;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencivic.signalos.domain.CivicComment;
import org.opencivic.signalos.domain.Community;
import org.opencivic.signalos.domain.CommunityBlogPost;
import org.opencivic.signalos.domain.User;
import org.opencivic.signalos.repository.CivicCommentRepository;
import org.opencivic.signalos.repository.CommunityBlogPostRepository;
import org.opencivic.signalos.repository.CommunityRepository;
import org.opencivic.signalos.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:communityblogcommentcountsitdb;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityBlogCommentCountsIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CivicCommentRepository commentRepository;

    @Autowired
    private CommunityBlogPostRepository blogPostRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityRepository communityRepository;

    /** A post points at a real author and a real community; random ids are orphans, not posts. */
    private UUID authorId;
    private UUID communityId;

    @BeforeEach
    void createAuthorAndCommunity() {
        User author = new User("blog_author", "{noop}pw", "blog-author@test.dev", "ROLE_CITIZEN");
        author.setEnabled(true);
        author.setVerified(true);
        authorId = userRepository.save(author).getId();

        Community community = new Community();
        community.setName("Blog District");
        community.setSlug("blog-district");
        communityId = communityRepository.save(community).getId();
    }

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        blogPostRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "citizen", roles = {"CITIZEN"})
    void shouldReturnCommentCountsForRequestedBlogPostsIncludingZeroes() throws Exception {
        CommunityBlogPost post1 = createPost(communityId, authorId, "Post 1");
        CommunityBlogPost post2 = createPost(communityId, authorId, "Post 2");
        CommunityBlogPost post3 = createPost(communityId, authorId, "Post 3");

        commentRepository.save(new CivicComment(post1.getId(), "BLOG", authorId, "Comment A"));
        commentRepository.save(new CivicComment(post1.getId(), "BLOG", authorId, "Comment B"));
        commentRepository.save(new CivicComment(post2.getId(), "BLOG", authorId, "Comment C"));

        String query = "/api/community/blog/comments/count?postIds=" +
            post1.getId() + "," + post2.getId() + "," + post3.getId();

        mockMvc.perform(get(query))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$['" + post1.getId() + "']").value(2))
            .andExpect(jsonPath("$['" + post2.getId() + "']").value(1))
            .andExpect(jsonPath("$['" + post3.getId() + "']").value(0));
    }

    private CommunityBlogPost createPost(UUID communityId, UUID authorId, String title) {
        CommunityBlogPost post = new CommunityBlogPost();
        post.setCommunityId(communityId);
        post.setAuthorId(authorId);
        post.setTitle(title);
        post.setContent("Body for " + title);
        post.setStatusTag("IN_PROGRESS");
        return blogPostRepository.save(post);
    }
}
