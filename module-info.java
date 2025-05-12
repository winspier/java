open module java_solutions {
    requires java.compiler;
    requires java.logging;

    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.arrayset;
    requires info.kgeorgiy.java.advanced.lambda;
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.walk;
    requires info.kgeorgiy.java.advanced.implementor.tools;
    requires info.kgeorgiy.java.advanced.iterative;
    requires java.desktop;
    requires info.kgeorgiy.java.advanced.mapper;

    exports info.kgeorgiy.ja.koloskov.implementor;
    exports info.kgeorgiy.ja.koloskov.arrayset;
    exports info.kgeorgiy.ja.koloskov.lambda;
    exports info.kgeorgiy.ja.koloskov.student;
    exports info.kgeorgiy.ja.koloskov.walk;
}
