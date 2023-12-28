package net.yakclient.archive.mapper.transform

import net.yakclient.archive.mapper.ArchiveMapping
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Remapper

// TODO do this eventually, hard with the way they have implemented Invoke Dynamic instructions though
//public class ArchiveRemapper(
//    private val mappings: ArchiveMapping,
//    private val destNamespace: String
//) : Remapper() {
//    override fun mapMethodName(owner: String, name: String, descriptor: String): String? {
//        return mappings.mapMethodName(owner, name, descriptor, destNamespace)
//    }
//
//    override fun mapInvokeDynamicMethodName(name: String?, descriptor: String?): String? {
//        return name
//    }
//
//    override fun mapRecordComponentName(
//        owner: String?, name: String, descriptor: String?
//    ): String {
//        return name
//    }
//
//    override fun mapFieldName(owner: String?, name: String?, descriptor: String?): String? {
//        return name
//    }
//
//
//    override fun mapPackageName(name: String): String {
//        return name
//    }
//
//
//    override fun mapModuleName(name: String): String {
//        return name
//    }
//
//    override fun map(internalName: String?): String? {
//        return internalName
//    }
//}