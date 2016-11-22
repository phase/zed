import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import java.io.File
import java.io.StringWriter

fun main(args: Array<String>) {
    Vertx.vertx().deployVerticle(Zed(TemplateManager()))
    println("Server started: http://localhost:8080/")
}

class Zed(val templateManager: TemplateManager) : AbstractVerticle() {

    val router: Router

    init {
        router = Router.router(vertx)

        router.route("/static/:file").handler { r ->
            val file = File("static/" + r.request().getParam("file"))
            if (file.exists())
                r.response().end(file.readLines().joinToString("\n"))
            else
                r.response().end()
        }

        router.route().failureHandler { r ->
            if (r.statusCode() === 404) {
                val writer = StringWriter()
                templateManager.error.evaluate(writer)
                r.response().end(writer.toString())
            } else
                r.next()
        }

        router.route("/").handler { r ->
            val writer = StringWriter()
            val content: MutableMap<String, Any> = mutableMapOf(
                    "posts" to listOf(
                            Post("some stupid hacker news clone", "https://github.com/phase/zed", "phase"),
                            Post("GitHub.com - Best site for open source software", "https://github.com", "phase")
                    ))
            templateManager.home.evaluate(writer, content)
            r.response().end(writer.toString())
        }

    }

    override fun start(fut: Future<Void>) {
        vertx
                .createHttpServer()
                .requestHandler({ r -> router.accept(r) })
                .listen(8080) { result ->
                    if (result.succeeded()) fut.complete()
                    else fut.fail(result.cause())
                }
    }

}
