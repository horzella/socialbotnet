package modules.post.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import modules.error.InputTooLongException;
import modules.post.model.Post;
import modules.post.service.PostService;
import modules.user.model.User;
import modules.user.service.UserService;
import modules.util.DecodeParams;
import modules.util.Renderer;
import org.apache.commons.beanutils.BeanUtils;
import org.eclipse.jetty.util.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

public class PostController {
  static final Logger logger = LoggerFactory.getLogger(PostController.class);
  private static final Map<String, String> acceptedSorts = new HashMap<>();

  static {
    acceptedSorts.put("likes", "mostliked");
    acceptedSorts.put("trending", "trending");
    acceptedSorts.put("time", "recent");
  }

  private PostService postService;
  private UserService userService;

  public PostController(PostService postService, UserService userService) {
    this.postService = postService;
    this.userService = userService;
  }

  public String getPosts(Request req, Response res) {
    Map<String, Object> model = new HashMap<>();
    User user = userService.getAuthenticatedUser(req);
    if (user != null) {
      model.put("authenticatedUser", user);
      model.put("postsLikedByUser", postService.getPostsLikedByUser(user));
    }

    String sortBy = req.queryParams("sortby");

    if (acceptedSorts.containsKey(sortBy)) {

      // explicit sort param
      List<Post> posts = postService.getWallPostsSorted(sortBy, false, 50);

      model.put(acceptedSorts.get(sortBy), posts);
      model.put("sortby", sortBy);
    } else {

      // fallback: 3 trending posts, then newest
      List<Post> trending = postService.getWallPostsSorted("trending", false, 3);
      List<Post> posts = postService.getWallPostsSorted(null, false, 50);

      model.put("trending", trending);
      model.put("recent", posts);
    }

    return Renderer.render(model, "posts/wall.page.ftl");
  }

  public String likePost(Request req, Response res) {
    return handleLikeAndUnlike(true, req, res);
  }

  public String unlikePost(Request req, Response res) {
    return handleLikeAndUnlike(false, req, res);
  }

  private String handleLikeAndUnlike(boolean liked, Request req, Response res) {
    User authenticatedUser = userService.getAuthenticatedUser(req);
    if (authenticatedUser == null) {
      Spark.halt(401, "Du bist nicht angemeldet!");
      return null;
    }

    MultiMap<String> params = DecodeParams.decode(req);
    Post post = postService.getPostById(Integer.parseInt(params.getString("post")));
    if (post == null) {
      Spark.halt(400, "Post existiert nicht");
      return null;
    }
    if (liked) {
      postService.likePost(post, authenticatedUser);
    } else {
      postService.unlikePost(post, authenticatedUser);
    }
    if (req.headers("referer") != null) {
      res.redirect(req.headers("referer") + "#post-" + post.getId());
    } else {
      try {
        res.redirect(
            String.format(
                "/pinnwand/%s#post-%s",
                URLEncoder.encode(post.getUsername(), DecodeParams.ENCODING), post.getId()));
      } catch (UnsupportedEncodingException e) {
        logger.error("unsupported encoding UTF-8", e);
      }
    }
    return null;
  }

  public String createPost(Request req, Response res) {
    User authenticatedUser = userService.getAuthenticatedUser(req);
    if (authenticatedUser == null) {
      Spark.halt(401, "Du bist nicht angemeldet!");
      return null;
    }

    Post post = new Post();
    post.setUser(authenticatedUser);
    post.setPublishingDate(new Timestamp(System.currentTimeMillis()));
    try { // populate post attributes by params
      BeanUtils.populate(post, DecodeParams.decode(req));
      String username = req.params("username");
      if (username != null) {
        post.setWall(userService.getUserbyUsername(username));
        res.redirect("/pinnwand/" + URLEncoder.encode(username, DecodeParams.ENCODING));
      } else {
        post.setWall(authenticatedUser);
        res.redirect(
            "/pinnwand/"
                + URLEncoder.encode(authenticatedUser.getUsername(), DecodeParams.ENCODING));
      }
    } catch (Exception e) {
      Spark.halt(500);
      return null;
    }

    try {
      postService.addPost(post);
    } catch (InputTooLongException e) {

    }
    return null;
  }

  /** @return the acceptedsorts */
  public static Map<String, String> getAcceptedSorts() {
    return acceptedSorts;
  }
}
