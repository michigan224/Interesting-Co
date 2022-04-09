package edu.umich.interestingco.rememri

class Memri(var post_id: Int? = null,
            var owner_id: String? = null,
            var is_public: Boolean? = null,
            var is_friend: Boolean? = null,
            var media_url: String? = null,
            var timestamp: String? = null,
            var comments: Array<Comment>? = null,
            var location: Array<Double>? = null)

class Memris(var memris: Array<Memri>? = null)