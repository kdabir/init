@Grab('io.github.kdabir.directree:directree:0.3.0')
import static directree.DirTree.*

// -*- CLI PARSING -*-
def (options, arguments) = new CliBuilder(usage : "init [options] key_word", stopAtNonOption: false).with {

    h('help',     longOpt: 'help')
    l('list',     longOpt: 'list')
    o('output',   longOpt: 'output',   args:1,   argName:'path')
    v('version',  longOpt: 'version')
    
    
    parse(args).with {
        delegate || System.exit(0)
        if (h) {usage(); System.exit(0)}
        if (v) {println("1.0"); System.exit(0)}
        [delegate, arguments()]    
    }    
}

// -*- VARS SETTINGS -*-
def year        = Calendar.getInstance().get(Calendar.YEAR)
def user        = "git config user.name".execute().text
def root        = options.o ?: System.getProperty('user.dir')
def allMethdos  = this.class.declaredMethods.findAll{!it.synthetic}.name
def templates   = allMethdos.findAll{it.startsWith("tpl_")}.collect{it.replaceAll('^tpl_', '')}

// -*- TEMPLATE GROUPS -*-
def collections = [
    'gradle_groovy_projet': { ->
        build(root) {
            file "build.gradle"         , tpl_build_gradle_groovy_appl()
            file ".gitignore"           , tpl_gitignore()
            dir ('src')  { 
                file "Main.groovy"      , tpl_main_class_groovy()
            }
            dir ('test') { 
                file "MainSpec.groovy"  , tpl_spock_spec() 
            }
        }
    },

    'sharing_ready': { ->
        build(root) {
            file "LICENSE"          , tpl_mit_license(year, user)
            file 'README.md'        , tpl_readme('My awesome project')
            file ".gitattributes"   , tpl_gitattributes()
            file ".editorconfig"    , tpl_editorconfig()
        }
    },

    'html' : { ->
        build(root) { 
            file "index.html" , tpl_minimal_html()
        }
    }
]

if (options.l) {
    println """\
    |*** Projects *** 
    |${collections.keySet().join('\n')}
    |
    |*** Snippets *** 
    |${templates.join('\n')}
    |""".stripMargin().stripIndent()

    System.exit(0)
}

if (!arguments) {
    println "please a provide a template name"
    System.exit(1)
}

def keyword = arguments.first()
def matching = collections.findAll { k,v-> k.contains(keyword) }

if(matching.size() > 1) {
    println "multiple templates for $keyword"
    println matching.keySet().join("\n")
    System.exit(2)
}
    
if(matching.size() == 0) {
    println "no matching templates for $keyword"
    System.exit(3)
}


matching.each { key , directree_closure ->
    def tree = directree_closure.call()
    
    if (tree.walk { it.file.isFile() && it.file.exists() }.any()) {
        println "some file(s) already exist:"
        println tree.walk { it.file.exists()? it.file : null }.findAll().join("\n")
        println "No files written!"
    } else {
        tree.create()
    }
}


// -*- TEMPLATE METHODS BELOW THIS -*-

def tpl_gitignore(){"""\
*~
.DS_Store

*.class
*.jar
target/
build/
.gradle/
!gradle-wrapper.jar
.gradletasknamecache

.idea
*.iml 
*.ipr
*.iws
out/

node_modules
"""}

def tpl_gitattributes(){ '* text=auto'}


def tpl_readme(project){"""\
# ${project}

"""}

def tpl_mit_license(year, name) {"""\
The MIT License (MIT)

Copyright (c) ${year} ${name}

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
"""}


def tpl_editorconfig(int indent_size = 4) {"""\
# http://editorconfig.org/

root = true

[*]
indent_style = space
indent_size = ${indent_size}

end_of_line = lf
charset = utf-8
trim_trailing_whitespace = true
insert_final_newline = true

[*.{groovy, java, gradle}]
indent_size = 4

[*.md]
trim_trailing_whitespace = false

[*.{js, css, html, json, yaml, coffee}]
indent_size = 2
indent_style = space

[{Makefile, makefile}]
indent_style = tab
indent_size = 4
"""}

def tpl_minimal_html() {"""\
<!DOCTYPE html>
<html lang=en>
  <head>
    <meta charset="utf-8">

    <title>Minimal Html</title>    

    <link rel="stylesheet" href="styles.css">

    <style>
        /* inline css here */
        body {background-color:lightgrey;}
    </style>
    
    <script>
        /* inline js here */
        console.log("js works")
    </script>
  </head>
  <body>
    <h1>Minimal Html Document</h1>    

    <script src="script.js"></script>    
  </body>
</html>
"""}

def tpl_build_gradle_groovy_appl(){"""\
plugins {
    id 'groovy'
    id 'application'
}

mainClassName = 'Main'

repositories {
    jcenter()
}

dependencies {
    compile 'org.codehaus.groovy:groovy-all:2.4.6'
    testCompile 'org.spockframework:spock-core:1.0-groovy-2.4'
}

sourceSets {
    main.groovy.srcDirs = ['src']
    test.groovy.srcDirs = ['test']
}    

test {
    testLogging {
        events 'failed'
        exceptionFormat 'short'
    }
}
"""}

def tpl_main_class_groovy(){"""\
class Main {
    public static void main(String[] args) {
        println 'Hello world'
    }
}
"""}

def tpl_main_class_java(){"""\
class Main {
    public static void main(String[] args) {
        System.out.println('Hello world');
    }
}
"""}

def tpl_spock_spec() {"""\
import spock.lang.*

// Help with Spock:
// - http://spockframework.github.io/spock/docs/1.0/spock_primer.html
// - http://spockframework.github.io/spock/docs/1.0/data_driven_testing.html
// - http://spockframework.github.io/spock/docs/1.0/extensions.html

class MainSpec extends Specification {

    def setup() {}          // setup
    def cleanup() {}        // teardown
    def setupSpec() {}      // before-class
    def cleanupSpec() {}    // after-class

    def "spec in expect style"() {
        expect:
        Math.max(2,3) == 3
    }

    // @Ignore // @IgnoreRest
    def "spec in when-then style"() {
        when:
        def actual = Math.max(2,3)

        then:
        actual == 3
    }

    @Unroll
    def "example of data driven test max(#a, #b) = #c"() {
        expect:
        Math.max(a, b) == c

        where:
        a | b || c
        3 | 5 || 5
        7 | 0 || 7
        0 | 0 || 0
    }
}
"""}
