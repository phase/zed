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
            if (!file.exists())
                throw Exception("File '${file.name}' not found in static/")
            r.response().end(file.readLines().joinToString("\n"))
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
            templateManager.home.evaluate(writer)
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