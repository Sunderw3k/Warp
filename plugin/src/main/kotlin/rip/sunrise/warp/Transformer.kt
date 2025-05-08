package rip.sunrise.warp

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import java.io.File

object Transformer {
    fun transform(file: File) {
        val bytes = file.readBytes()

        val node = ClassReader(bytes).let {
            val node = ClassNode(Opcodes.ASM9)
            it.accept(node, ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
            node
        }

        node.methods.toList().forEach { method ->
            method.visibleAnnotations?.filter { it.desc == "Lrip/sunrise/warp/annotations/Invoker;" }
                ?.forEach { annotation ->
                    val clazz = annotation.values[1] as Type
                    val name = annotation.values[3] as String
                    val isTargetStatic = annotation.values[5] as Boolean

                    method.visibleAnnotations.remove(annotation)

                    val arguments = Type.getMethodType(method.desc).argumentTypes

                    // Add MethodHandle field
                    node.fields.add(
                        FieldNode(
                            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL,
                            "INVOKER_$name",
                            "Ljava/lang/invoke/MethodHandle;",
                            null,
                            null
                        )
                    )

                    // Replace method instructions to invoke MethodHandle
                    method.instructions = InsnList().apply {
                        // Load handle
                        add(
                            FieldInsnNode(
                                Opcodes.GETSTATIC,
                                node.name,
                                "INVOKER_$name",
                                "Ljava/lang/invoke/MethodHandle;"
                            )
                        )

                        // Load arguments
                        val isVirtual = method.access and Opcodes.ACC_STATIC == 0
                        arguments.forEachIndexed { index, type ->
                            add(VarInsnNode(type.getOpcode(Opcodes.ILOAD), index + if (isVirtual) 1 else 0))
                        }

                        // Call invokeExact
                        add(
                            MethodInsnNode(
                                Opcodes.INVOKEVIRTUAL,
                                "java/lang/invoke/MethodHandle",
                                "invokeExact",
                                method.desc,
                                false
                            )
                        )

                        // Return
                        add(InsnNode(Type.getMethodType(method.desc).returnType.getOpcode(Opcodes.IRETURN)))
                    }

                    val patch = InsnList().apply {
                        // Load class literal
                        add(LdcInsnNode(clazz))

                        // Load method name
                        add(LdcInsnNode(name))

                        val searchArguments = if (isTargetStatic) arguments else arguments.drop(1).toTypedArray()

                        // Load args
                        add(searchArguments.size.getEffectiveIntLoadNode())
                        add(TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Class"))

                        searchArguments.forEachIndexed { index, type ->
                            add(InsnNode(Opcodes.DUP))
                            add(index.getEffectiveIntLoadNode())
                            add(LdcInsnNode(type))
                            add(InsnNode(Opcodes.AASTORE))
                        }

                        // Get Method
                        add(
                            MethodInsnNode(
                                Opcodes.INVOKEVIRTUAL,
                                "java/lang/Class",
                                "getDeclaredMethod",
                                "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
                                false
                            )
                        )

                        // setAccessible
                        add(InsnNode(Opcodes.DUP))
                        add(InsnNode(Opcodes.ICONST_1))
                        add(
                            MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "rip/sunrise/warp/UnsafeUtils",
                                "setAccessibleUnsafe",
                                "(Ljava/lang/reflect/Method;Z)V",
                                false
                            )
                        )

                        // Unreflect
                        add(
                            MethodInsnNode(
                                Opcodes.INVOKESTATIC,
                                "java/lang/invoke/MethodHandles",
                                "lookup",
                                "()Ljava/lang/invoke/MethodHandles\$Lookup;",
                                false
                            )
                        )
                        add(InsnNode(Opcodes.SWAP))
                        add(
                            MethodInsnNode(
                                Opcodes.INVOKEVIRTUAL,
                                "java/lang/invoke/MethodHandles\$Lookup",
                                "unreflect",
                                "(Ljava/lang/reflect/Method;)Ljava/lang/invoke/MethodHandle;",
                                false
                            )
                        )

                        add(
                            FieldInsnNode(
                                Opcodes.PUTSTATIC,
                                node.name,
                                "INVOKER_$name",
                                "Ljava/lang/invoke/MethodHandle;"
                            )
                        )
                    }

                    (node.methods.firstOrNull { it.name == "<clinit>" } ?: run {
                        MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null).apply {
                            instructions = InsnList().apply { add(InsnNode(Opcodes.RETURN)) }
                            node.methods.add(this)
                        }
                    }).instructions.insert(patch)
                }
        }

        val modifiedBytes = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS).let {
            node.accept(it)
            it.toByteArray()
        }

        file.writeBytes(modifiedBytes)
    }
}

fun Int.getEffectiveIntLoadNode(): AbstractInsnNode = when (this) {
    in -1..5 -> InsnNode(Opcodes.ICONST_0 + this)
    in 5..255 /* 5 to 2^8-1*/ -> IntInsnNode(Opcodes.BIPUSH, this)
    in 256..65_535 /* 2^8 to 2^16-1*/ -> IntInsnNode(Opcodes.SIPUSH, this)
    else -> LdcInsnNode(this)
}