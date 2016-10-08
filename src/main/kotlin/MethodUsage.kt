
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.expr.MethodCallExpr
import me.tomassetti.symbolsolver.javaparsermodel.JavaParserFacade
import me.tomassetti.symbolsolver.model.resolution.TypeSolver
import me.tomassetti.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import me.tomassetti.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import me.tomassetti.symbolsolver.resolution.typesolvers.JreTypeSolver
import java.io.File

fun typeSolver() : TypeSolver {
    val combinedTypeSolver = CombinedTypeSolver()
    combinedTypeSolver.add(JreTypeSolver())
    combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-core")))
    combinedTypeSolver.add(JavaParserTypeSolver(File("src/main/resources/javaparser-generated-sources")))
    return combinedTypeSolver
}

class DirExplorer(private val filter: DirExplorer.Filter, private val fileHandler: DirExplorer.FileHandler) {
    interface FileHandler {
        fun handle(level: Int, path: String, file: File)
    }

    interface Filter {
        fun interested(level: Int, path: String, file: File): Boolean
    }

    fun explore(root: File) {
        explore(0, "", root)
    }

    private fun explore(level: Int, path: String, file: File) {
        if (file.isDirectory) {
            for (child in file.listFiles()) {
                explore(level + 1, path + "/" + child.name, child)
            }
        } else {
            if (filter.interested(level, path, file)) {
                fileHandler.handle(level, path, file)
            }
        }
    }

}

var ok = 0
var ko = 0

fun processJavaFile(file: File, javaParserFacade: JavaParserFacade) {
    println(file)
    JavaParser.parse(file).descendantsOfType(MethodCallExpr::class.java).forEach {
        println(" * $it ")
        try {
            println("  -> ${javaParserFacade.solve(it).correspondingDeclaration.name}")
            ok++
            println("OK $ok KO $ko")
        } catch (e: Exception) {
            println("ERR ${e.message}")
            ko++
            println("OK $ok KO $ko")
        }
    }
}

fun main(args:Array<String>) {
    val javaFiles = findJavaFiles(File("src/main/resources/javaparser-core"))
    val javaParserFacade = JavaParserFacade.get(typeSolver())
    javaFiles.forEach { processJavaFile(it, javaParserFacade) }
    println("OK $ok KO $ko")
}