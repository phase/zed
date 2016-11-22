import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.template.PebbleTemplate

class TemplateManager {

    val home: PebbleTemplate
    val error: PebbleTemplate
    val register: PebbleTemplate

    init {
        val engine = PebbleEngine.Builder().build()
        home = engine.getTemplate("template/home.html")
        error = engine.getTemplate("template/404.html")
        register = engine.getTemplate("template/register.html")
    }

}
