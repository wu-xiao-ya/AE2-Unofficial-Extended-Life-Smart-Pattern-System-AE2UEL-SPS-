package com.lwx1145.sampleintegration.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ListIterator;

/**
 * MMCE compatibility shim for JsonUtils#fromJson(Type, boolean) signature mismatch.
 */
public class MMCEJsonUtilsCompatTransformer implements IClassTransformer {

    private static final String JSON_UTILS_OWNER = "net/minecraft/util/JsonUtils";
    private static final String HOOK_OWNER = "com/lwx1145/sampleintegration/compat/JsonUtilsCompat";
    private static final String STRING_TYPE_DESC = "(Lcom/google/gson/Gson;Ljava/lang/String;Ljava/lang/reflect/Type;Z)Ljava/lang/Object;";
    private static final String READER_TYPE_DESC = "(Lcom/google/gson/Gson;Ljava/io/Reader;Ljava/lang/reflect/Type;Z)Ljava/lang/Object;";
    private static final String READER_CLASS_DESC = "(Lcom/google/gson/Gson;Ljava/io/Reader;Ljava/lang/Class;)Ljava/lang/Object;";
    private static final String GET_STRING_DEFAULT_DESC = "(Lcom/google/gson/JsonObject;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;";
    private static final String GET_JSON_ARRAY_DESC = "(Lcom/google/gson/JsonObject;Ljava/lang/String;)Lcom/google/gson/JsonArray;";
    private static final String GET_JSON_ARRAY_DEFAULT_DESC = "(Lcom/google/gson/JsonObject;Ljava/lang/String;Lcom/google/gson/JsonArray;)Lcom/google/gson/JsonArray;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || transformedName == null) {
            return basicClass;
        }
        if (!transformedName.startsWith("hellfirepvp.modularmachinery.")
            && !transformedName.startsWith("github.kasuminova.mmce.")) {
            return basicClass;
        }

        try {
            ClassNode classNode = new ClassNode();
            new ClassReader(basicClass).accept(classNode, 0);

            int replaced = 0;
            for (MethodNode method : classNode.methods) {
                ListIterator<AbstractInsnNode> it = method.instructions.iterator();
                while (it.hasNext()) {
                    AbstractInsnNode insn = it.next();
                    if (!(insn instanceof MethodInsnNode)) {
                        continue;
                    }
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    if (methodInsn.getOpcode() != Opcodes.INVOKESTATIC
                        || !JSON_UTILS_OWNER.equals(methodInsn.owner)) {
                        continue;
                    }
                    if (!shouldPatch(methodInsn.name, methodInsn.desc)) {
                        continue;
                    }

                    MethodInsnNode replacement = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        HOOK_OWNER,
                        methodInsn.name,
                        methodInsn.desc,
                        false
                    );
                    method.instructions.set(methodInsn, replacement);
                    replaced++;
                }
            }

            if (replaced == 0) {
                return basicClass;
            }

            // Preserve original frames; only static call sites are replaced with same descriptors.
            ClassWriter classWriter = new ClassWriter(0);
            classNode.accept(classWriter);
            System.out.println("[MMCEJsonUtilsCompatTransformer] Patched " + transformedName
                + " (" + replaced + " JsonUtils#fromJson call)");
            return classWriter.toByteArray();
        } catch (Throwable t) {
            System.err.println("[MMCEJsonUtilsCompatTransformer] Transform failed for " + transformedName + ": " + t.getMessage());
            t.printStackTrace();
            return basicClass;
        }
    }

    private static boolean shouldPatch(String methodName, String desc) {
        if ("fromJson".equals(methodName)) {
            return STRING_TYPE_DESC.equals(desc)
                || READER_TYPE_DESC.equals(desc)
                || READER_CLASS_DESC.equals(desc);
        }
        if ("getString".equals(methodName)) {
            return GET_STRING_DEFAULT_DESC.equals(desc);
        }
        if ("getJsonArray".equals(methodName)) {
            return GET_JSON_ARRAY_DESC.equals(desc)
                || GET_JSON_ARRAY_DEFAULT_DESC.equals(desc);
        }
        return false;
    }
}
