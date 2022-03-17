package edu.umich.interestingco.rememri

class Memri(var pin_id: Int? = null,
            var owner_id: String? = null,
            var is_public: Boolean? = null,
            var media_url: String? = null,
            var timestamp: String? = null,
            var comments: CommentStore? = null,
            var location: Array<Int>? = null)