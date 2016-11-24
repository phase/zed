import com.lambdaworks.crypto.SCryptUtil
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.asyncsql.AsyncSQLClient
import io.vertx.ext.asyncsql.MySQLClient
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import java.io.File
import java.io.StringWriter


fun main(args: Array<String>) {
    Vertx.vertx().deployVerticle(Zed(TemplateManager()))
}

class Zed(val templateManager: TemplateManager) : AbstractVerticle() {

    var users: MutableList<User> = mutableListOf()

    fun createSQLConnection(): AsyncSQLClient {
        val config = File("config.json")
        assert(config.exists())
        val configJson = JsonObject(config.readLines().joinToString("\n"))
        val client = MySQLClient.createShared(vertx, configJson.getJsonObject("mysql"), "ZedMySQLPool")

        // Initial Configuration
        client.getConnection { res ->
            if (res.succeeded()) {
                val tableExists = """
                SELECT *
                FROM information_schema.tables
                WHERE table_schema = 'DB'
                    AND table_name = 'TABLE'
                LIMIT 1;
                """.replace("DB", configJson.getJsonObject("mysql").getString("database"))

                val connection = res.result()
                connection.query(tableExists.replace("TABLE", "users"), { r ->
                    if (r.succeeded()) {
                        val result = r.result()
                        if (result.numRows == 0) {
                            // table doesn't exist
                            println("Creating Users Table")
                            val usersSql = File("sql/users.sql")
                            assert(usersSql.exists())
                            connection.execute(usersSql.readLines().joinToString("\n"), { r ->
                                assert(r.succeeded(), { r.cause() })
                            })
                        }
                    }
                })
            } else {
                throw Exception("Couldn't connect to MySQL Database.\n" + res.cause().message)
            }
        }

        return client
    }

    fun createRouter(client: AsyncSQLClient): Router {
        val router = Router.router(vertx)

        // Used for forms
        router.route().handler(BodyHandler.create())

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
                            Post("some stupid hacker news clone", "https://github.com/phase/zed", User(0, "phase", "null")),
                            Post("GitHub.com - Best site for open source software", "https://github.com", User(0, "phase", "null"))
                    ))
            templateManager.home.evaluate(writer, content)
            r.response().end(writer.toString())
        }

        router.get("/register/").handler { r ->
            val writer = StringWriter()
            templateManager.register.evaluate(writer)
            r.response().end(writer.toString())
        }

        router.post("/register/").handler { r ->
            val username = r.request().getParam("username")
            val password = r.request().getParam("password")
            val hashed = SCryptUtil.scrypt(password, 16384, 8, 1)
            client.getConnection { r ->
                assert(r.succeeded(), { r.cause() })
                val c = r.result()
                c.execute("""insert into users (name, password)
                        values ("#U", "#P")"""
                        .replace("#U", username)
                        .replace("#P", hashed), { r ->
                    assert(r.succeeded(), { r.cause() })
                    c.query("select id from users order by id desc limit 1", { r ->
                        assert(r.succeeded(), { r.cause() })
                        val result = r.result()
                        val id = result.results[result.numRows - 1].getInteger(result.results.size - 1)
                        println("New User: $id: $username ($hashed)")
                        users.add(User(id, username, hashed))
                    })
                })

            }
            r.reroute("/")
        }

        return router
    }

    override fun start(fut: Future<Void>) {
        val client = createSQLConnection()
        val router = createRouter(client)
        vertx
                .createHttpServer()
                .requestHandler({ r -> router.accept(r) })
                .listen(8080) { result ->
                    if (result.succeeded()) fut.complete()
                    else fut.fail(result.cause())
                }
        println("Server started: http://localhost:8080/")
    }

}
