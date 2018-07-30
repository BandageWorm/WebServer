package mcs;

/**
 * Pair of post and cmnt.
 * Created by kurtg on 17/2/8.
 */
public class Pair {
    private String postID;
    private String post;
    private String cmntID;
    private String cmnt;

    Pair(String postID, String post, String cmntID, String cmnt) {
        this.postID = postID;
        this.post = post;
        this.cmntID = cmntID;
        this.cmnt = cmnt;
    }

    public String getPostID() {
        return postID;
    }

    public String getPost() {
        return post;
    }

    public String getCmntID() {
        return cmntID;
    }

    public String getCmnt() {
        return cmnt;
    }
}
