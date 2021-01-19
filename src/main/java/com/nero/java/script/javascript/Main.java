package com.nero.java.script.javascript;

import jdk.nashorn.internal.objects.NativeArray;
import jdk.nashorn.internal.objects.NativeObject;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");
        //scriptEngineManager.getEngineByName("js");
        //engine.eval("print('hello')");

        //testScriptVariables(engine);
        //testInvokeScriptMethod(engine);
        //testScriptInterface(engine);
        //testUsingJDKClasses(engine);
        //testRunJSFile(engine);
        testImportJDK(engine);
    }

    /**
     * JavaScript使用Java类型的变量
     *
     * @param engine 脚本引擎
     * @throws ScriptException
     */
    public static void testScriptVariables(ScriptEngine engine) throws ScriptException {
        Map<String, Object> map = new HashMap<>();
        map.put("a", "将java参数 变为脚本变量");
        engine.put("map", map);
        engine.put("user", new User("nero", 18));
        //在下面脚本中可以直接使用Java类型的变量
        engine.eval("map.put('b',new Date()); print(map.get('a') +'  '+ map.get('b')+' '+user.getName());");
    }

    /**
     * 执行JS函数,运算字符串公式.
     *
     * @param engine 脚本引擎
     * @throws Exception
     */
    public static void testInvokeScriptMethod(ScriptEngine engine) throws Exception {
        String script = "function calc(num1,num2) { return (num1+num2)/2;}  function test(){return 12*6-9/2 +5;}";
        engine.eval(script);
        if (!(engine instanceof Invocable))
            throw new RuntimeException("engine type is not support");
        Invocable inv = (Invocable) engine;
        Double res = (Double) inv.invokeFunction("test");
        System.out.println("res:" + res);
        Double calc = (Double) inv.invokeFunction("calc", 10, 5);
        System.out.println("calc:" + calc);
    }

    /**
     * JS实现java接口.
     *
     * @param engine 脚本引擎
     * @throws ScriptException
     */
    public static void testScriptInterface(ScriptEngine engine) throws ScriptException {
        String script = "var obj = new Object();obj.run = function() { print('run method called');}";
        engine.eval(script);
        Object obj = engine.get("obj");
        if (!(engine instanceof Invocable))
            throw new RuntimeException("engine type is not support");
        Invocable inv = (Invocable) engine;
        Runnable r = inv.getInterface(obj, Runnable.class);
        Thread th = new Thread(r);
        th.start();
    }

    /**
     * js 使用java类.
     *
     * @param engine 脚本引擎
     * @throws Exception
     */
    public static void testUsingJDKClasses(ScriptEngine engine) throws Exception {
        // Packages是脚本语言里的一个全局变量,专用于访问JDK的package
        String js = "function doSwing(t){var f=new Packages.javax.swing.JFrame(t);f.setSize(400,300);f.setVisible(true);}";
        // String js =
        // "function doSwing(t){var f=Packages.java.util.UUID.randomUUID();print(f)}";
        engine.eval(js);
        if (!(engine instanceof Invocable))
            throw new RuntimeException("engine type is not support");
        Invocable inv = (Invocable) engine;
        inv.invokeFunction("doSwing", "Scripting Swing");
    }

    /**
     * 运行js文件
     *
     * @param engine 脚本引擎
     * @throws ScriptException
     */
    public static void testRunJSFile(ScriptEngine engine) throws ScriptException {
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream("abc.js");
        assert inputStream != null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        engine.eval(reader);
    }

    public static void testImportJDK(ScriptEngine engine) throws ScriptException {
        StringBuilder script = new StringBuilder();
        //通过Java.type('class')导入Java类型
        script.append("var System = Java.type('java.lang.System');");
        script.append("print(System.getProperty('os.name').search('Windows') > -1 ? 'ServiceWindows' : 'ServiceOther')");
        engine.eval(script.toString());
    }

    /**
     * 将前台传过来的json数组构造成java的List<Map<String,Object>>,然后就可以随心所欲的使用该list了
     * 当然可以使用第三方jar采用json to bean的方式，而且这种方案更优雅，但是需要引入第三方库
     *
     * @throws NoSuchMethodException
     * @throws ScriptException
     */
    public static void test(ScriptEngine engine) throws ScriptException, NoSuchMethodException {

        // String json =
        // "{'key1':'a','son':{'dd':'dd','a':8},'ran':Math.random()},{'key3':'xor'}";
        String json = "{'key1':'a','son':[{'dd':'dd'},{'dd1':'dd1'}],'ran':Math.random()},{'key3':'xor'}";
        NativeArray json2array = json2array(engine, "[" + json + "]");
        List<Map<String, Object>> list = array2list(engine, json2array);
        System.out.println(list);

    }

    public static NativeArray json2array(ScriptEngine engine, String json) throws ScriptException, NoSuchMethodException {
        String script = "function hello() { return " + json + ";}";
        engine.eval(script);
        Invocable inv = (Invocable) engine;
        Object obj = inv.invokeFunction("hello");
        return (NativeArray) obj;
    }

    public static List<Map<String, Object>> array2list(ScriptEngine engine, NativeArray nativeArray) throws ScriptException, NoSuchMethodException {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        engine.put("list", list);
        engine.put("arr", nativeArray);
        StringBuffer sb = new StringBuffer("function dosomething(){      ");
        sb.append(" for (n=0; n< arr.length; n++){  ");
        sb.append("     var map=new Packages.java.util.HashMap();  ");
        sb.append("     for (i in arr[n]){ map.put(i,arr[n][i]);");
        sb.append("  }     list.add(map);     }  }  ");
        engine.eval(sb.toString());
        Invocable inv = (Invocable) engine;
        inv.invokeFunction("dosomething");
        for (Map<String, Object> map : list) {
            Set<Map.Entry<String, Object>> entrySet = map.entrySet();
            for (Map.Entry<String, Object> entry : entrySet) {
                Object object = entry.getValue();
                if (object instanceof NativeArray) {
                    map.put(entry.getKey(), array2list(engine, (NativeArray) object));
                } else if (object instanceof NativeObject) {
                    map.put(entry.getKey(), obj2map(engine, object));
                }
            }
        }
        return list;
    }

    public static Map<String, Object> obj2map(ScriptEngine engine, Object nativeObject) throws ScriptException, NoSuchMethodException {
        Map<String, Object> map = new HashMap<String, Object>();
        engine.put("map", map);
        engine.put("obj", nativeObject);
        String script = " function dosomething(){ for (i in obj){  map.put(i,obj[i]);  } }";
        engine.eval(script);
        Invocable inv = (Invocable) engine;
        inv.invokeFunction("dosomething");
        return map;
    }


    private static class User {
        private String name;
        private int age;

        User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }
}
