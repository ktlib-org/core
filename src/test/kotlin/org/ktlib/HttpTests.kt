package org.ktlib

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File

data class Post(val userId: Int, val id: Int?, val title: String?)

class HttpTests : StringSpec({
    val site = "https://jsonplaceholder.typicode.com"

    "download" {
        val file = "$site/posts/1".httpDownload(File("build"))

        file.readText().fromJson<Post>().id shouldBe 1
    }

    "get" {
        val result = "$site/posts/1".httpGet<Post>()

        result.id shouldBe 1
    }

    "get string" {
        val result = "$site/posts/1".httpGet<String>()

        result::class shouldBe String::class
    }

    "get nothing" {
        val result = "$site/posts/1".httpGet<Unit>()

        result::class shouldBe Unit::class
    }

    "get with params" {
        val result = "$site/comments".httpGet<List<Post>>(mapOf("postId" to 1))

        val greaterThanOne = result.size > 1
        greaterThanOne shouldBe true
    }

    "patch" {
        val result = "$site/posts/1".httpPatch<Post>(mapOf("title" to "newTitle"))

        result.id shouldBe 1
        result.title shouldBe "newTitle"
    }

    "post" {
        val result = "$site/posts".httpPost<Post>(Post(userId = 1, title = "myTitle", id = null))

        result.id shouldBe 101
        result.title shouldBe "myTitle"
    }

    "post form" {
        val result = "$site/posts".httpPostForm<Post>(mapOf("userId" to 1, "title" to "myTitle"))

        result.id shouldBe 101
        result.title shouldBe "myTitle"
    }

    "post with nothing returned" {
        val result = Post(userId = 1, title = "myTitle", id = null).postToUrl<Unit>("$site/posts")

        result::class shouldBe Unit::class
    }

    "put" {
        val result = "$site/posts/1".httpPut<Post>(Post(userId = 1, title = "myTitle", id = 1))

        result.id shouldBe 1
        result.title shouldBe "myTitle"
    }
})
