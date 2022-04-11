package edu.umich.interestingco.rememri

class Comment(var comment_id: Int? = null,
              var text: String? = null,
              var timestamp: String? = null,
              var post_id: Int? = null,
              var owner_id: String? = null,
              var is_owned_by_user: Boolean? = null) {
}
