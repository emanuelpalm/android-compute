package se.ltu.emapal.compute.util.media.schema

import se.ltu.emapal.compute.util.media.MediaDecoder
import se.ltu.emapal.compute.util.media.MediaEncodable
import se.ltu.emapal.compute.util.media.MediaEncoder
import se.ltu.emapal.compute.util.media.MediaEncoderMap

import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

/**
 * A schema used for verifying the shape of some [MediaDecoder].
 */
interface MediaSchema : MediaEncodable {
    /** Validates given value and produces report.  */
    fun verify(value: MediaDecoder): MediaReport

    /**
     * Base class for [MediaDecoder] schemas.
     */
    abstract class TypeEntity<T> : MediaEncodable {
        private val mutableRequirements = TreeSet<MediaRequirement>({ a, b -> a.name.compareTo(b.name) })
        private var mutableIsOptional = false

        /** Expected entity media type.  */
        protected abstract val type: MediaDecoder.Type

        /** Concrete type reference.  */
        protected abstract val self: T

        /** Schema requirements. */
        protected val requirements: Set<MediaRequirement>
            get() = mutableRequirements

        /** Whether or not schema entity is optional. */
        protected val isOptional: Boolean
            get() = mutableIsOptional

        /** Sets whether or not value is optional. Defaults to `false`.  */
        fun setOptional(isOptional: Boolean = true): T {
            this.mutableIsOptional = isOptional
            return self
        }

        /**
         * Adds arbitrary requirement.
         *
         * The requirement is guaranteed to be executed after it is known that a provided value is
         * not null and is of [type].
         *
         * If a requirement with the same name already exists, an [IllegalArgumentException] is
         * thrown. This method is used internally by this and extending classes, which means that
         * custom requirements live in the same namespace as standard ones. The special requirement
         * names `type` and `optional` are reserved and may never be used.
         */
        fun add(requirement: MediaRequirement): T {
            assert(requirement.name != "type" && requirement.name != "optional")

            if (!mutableRequirements.add(requirement)) {
                throw IllegalArgumentException("Requirement with name '" + requirement.name + "' already provided.")
            }
            return self
        }

        /** Validates identified value.  */
        internal open fun verify(entity: String, value: MediaDecoder?): Collection<MediaViolation> {
            val violations = ArrayList<MediaViolation>(4)
            if (value == null) {
                if (!mutableIsOptional) {
                    violations.add(MediaViolation(entity, REQUIREMENT_OPTIONAL_FALSE))
                }
            } else if (type !== MediaDecoder.Type.UNDEFINED && value.type !== type) {
                violations.add(MediaViolation(entity, MediaRequirement("type", type)))

            } else {
                requirements
                        .filter { !it.predicate(value) }
                        .forEach({ violations.add(MediaViolation(entity, it)) })
            }
            return violations
        }

        override val encodable: (MediaEncoder) -> Unit
            get () = {
                it.encodeMap({ encodeRequirements(it) })
            }

        /** Encodes entity requirements into given map.  */
        protected fun encodeRequirements(mapEncoder: MediaEncoderMap) {
            mapEncoder
                    .add("type", type.toString())
                    .add("optional", mutableIsOptional)

            if (requirements.size > 0) {
                mapEncoder.addList("requirements", { list ->
                    requirements.forEach({ list.add(it) })
                })
            }
        }

        companion object {
            protected val REQUIREMENT_OPTIONAL_FALSE = MediaRequirement("optional", "false")
            protected val REQUIREMENT_EXPECTED_TRUE = MediaRequirement("expected", "true")
        }
    }

    /**
     * [MediaDecoder] null type schema.
     */
    class TypeNull : TypeEntity<TypeNull>() {
        override val type: MediaDecoder.Type
            get () = MediaDecoder.Type.NULL

        override val self: TypeNull
            get () = this
    }

    /**
     * [MediaDecoder] boolean type schema.
     */
    class TypeBoolean : TypeEntity<TypeBoolean>() {
        override val type: MediaDecoder.Type
            get () = MediaDecoder.Type.BOOLEAN

        override val self: TypeBoolean
            get () = this
    }

    /**
     * [MediaDecoder] number type schema.
     */
    class TypeNumber : TypeEntity<TypeNumber>() {
        override val type: MediaDecoder.Type
            get () = MediaDecoder.Type.NUMBER

        override val self: TypeNumber
            get () = this

        /** Requires number to be equal to or larger than provided minimum.  */
        fun setMinimum(minimum: Long): TypeNumber {
            return add(MediaRequirement({ value -> value.toLong() >= minimum }, "minimum", minimum))
        }

        /** Requires number to be equal to or larger than provided minimum.  */
        fun setMinimum(minimum: BigInteger): TypeNumber {
            return add(MediaRequirement({ value -> value.toBigInteger().compareTo(minimum) >= 0 }, "minimum", minimum))
        }

        /** Requires number to be equal to or larger than provided minimum.  */
        fun setMinimum(minimum: Double): TypeNumber {
            return add(MediaRequirement({ value -> value.toDouble() >= minimum }, "minimum", minimum))
        }

        /** Requires number to be equal to or larger than provided minimum.  */
        fun setMinimum(minimum: BigDecimal): TypeNumber {
            return add(MediaRequirement({ value -> value.toBigDecimal().compareTo(minimum) >= 0 }, "minimum", minimum))
        }

        /** Requires number to be equal to or smaller than provided maximum.  */
        fun setMaximum(maximum: Long): TypeNumber {
            return add(MediaRequirement({ value -> value.toLong() <= maximum }, "maximum", maximum))
        }

        /** Requires number to be equal to or smaller than provided maximum.  */
        fun setMaximum(maximum: BigInteger): TypeNumber {
            return add(MediaRequirement({ value -> value.toBigInteger().compareTo(maximum) <= 0 }, "maximum", maximum))
        }

        /** Requires number to be equal to or smaller than provided maximum.  */
        fun setMaximum(maximum: Double): TypeNumber {
            return add(MediaRequirement({ value -> value.toDouble() <= maximum }, "maximum", maximum))
        }

        /** Requires number to be equal to or smaller than provided maximum.  */
        fun setMaximum(maximum: BigDecimal): TypeNumber {
            return add(MediaRequirement({ value -> value.toBigDecimal().compareTo(maximum) <= 0 }, "maximum", maximum))
        }
    }

    /**
     * [MediaDecoder] text type schema.
     */
    class TypeText : TypeEntity<TypeText>() {
        override val type: MediaDecoder.Type
            get () = MediaDecoder.Type.TEXT

        override val self: TypeText
            get () = this

        /** Requires text to match given regular expression.  */
        fun setRegex(regex: Regex): TypeText {
            return add(MediaRequirement({ value -> regex.matches(value.toText()) }, "regex", regex.pattern))
        }

        /** Requires text to match given regular expression.  */
        fun setRegex(regex: String): TypeText {
            return setRegex(Regex.fromLiteral(regex))
        }
    }

    /**
     * [MediaDecoder] BLOB type schema.
     */
    class TypeBlob : TypeEntity<TypeBlob>() {
        override val type: MediaDecoder.Type
            get () = MediaDecoder.Type.BLOB

        override val self: TypeBlob
            get () = this

        /** Requires BLOB to match the given byte size. */
        fun setSize(size: Int): TypeBlob {
            return add(MediaRequirement({ value -> value.toBlob().size == size }, "size", size))
        }
    }

    /**
     * [MediaDecoder] list type schema.
     */
    class TypeList : TypeEntity<TypeList>(), MediaSchema {
        private val schemaMap = TreeMap<Int, TypeEntity<*>>()
        private var schemaDefault: TypeEntity<*>? = null

        override val type: MediaDecoder.Type
            get () = MediaDecoder.Type.LIST

        override val self: TypeList
            get () = this

        /** Sets schema to apply only to list element at specified index offset.  */
        fun schemaElement(index: Int, schema: TypeEntity<*>): TypeList {
            schemaMap.getOrPut(index, { schema }).let {
                if (it !== schema) {
                    throw IllegalArgumentException("Schema already set for element $index.")
                }
            }
            return this
        }

        /**
         * Sets default schema.
         *
         * If no default schema is set, then only entries with schemas specified via
         * [schemaElement] are allowed.
         */
        fun schemaDefault(schema: TypeEntity<*>): TypeList {
            if (this.schemaDefault != null) {
                throw IllegalArgumentException("Default schema already set.")
            }
            this.schemaDefault = schema
            return this
        }

        /** Sets required list size, in elements.  */
        fun setSize(size: Int): TypeList {
            return add(MediaRequirement({ value -> value.toList().size === size }, "size", size))
        }

        override val encodable: (MediaEncoder) -> Unit
            get () = {
                it.encodeMap({
                    encodeRequirements(it)
                    if (!schemaMap.isEmpty()) {
                        it.addMap("elements", { elements ->
                            schemaMap.entries.forEach({ entry ->
                                elements.add("" + entry.key, entry.value)
                            })
                        })
                    }
                    val realSchemaDefault = schemaDefault
                    if (realSchemaDefault != null) {
                        it.add("default", realSchemaDefault)
                    }
                })
            }

        override fun verify(entity: String, value: MediaDecoder?): Collection<MediaViolation> {
            val violations = super.verify(entity, value)
            if (violations.size > 0) {
                return violations
            }
            val list = value!!.toList()
            return (0..list.size).map {
                val elementEntity = "$entity[$it]"
                val schema = schemaMap.getOrElse(it, { schemaDefault })
                if (schema == null) {
                    listOf(MediaViolation(elementEntity, MediaRequirement("expected", "true")))
                } else {
                    schema.verify(elementEntity, list[it])
                }
            }.flatten()
        }

        override fun verify(value: MediaDecoder): MediaReport {
            return MediaReport(verify("", value))
        }
    }

    /**
     * [MediaDecoder] map type schema.
     */
    class TypeMap : TypeEntity<TypeMap>(), MediaSchema {
        private val schemaMap = TreeMap<String, TypeEntity<*>>()
        private var schemaDefault: TypeEntity<*>? = null

        override val type: MediaDecoder.Type
            get () = MediaDecoder.Type.MAP

        override val self: TypeMap
            get () = this

        /** Sets schema applied only to map entry with specified key.  */
        fun schemaEntry(key: String, schema: TypeEntity<*>): TypeMap {
            schemaMap.getOrPut(key, { schema }).let {
                if (it !== schema) {
                    throw IllegalArgumentException("Schema already set with key $key.")
                }
            }
            return this
        }

        /**
         * Sets default schema.
         *
         * If no default schema is set, then only entries with schemas specified via [schemaEntry]
         * are allowed.
         */
        fun schemaDefault(schema: TypeEntity<*>): TypeMap {
            if (this.schemaDefault != null) {
                throw IllegalArgumentException("Default schema already set.")
            }
            this.schemaDefault = schema
            return this
        }

        /** Sets required map size, in entries.  */
        fun setSize(size: Int): TypeMap {
            return add(MediaRequirement({ value -> value.toMap().size === size }, "size", size))
        }

        override val encodable: (MediaEncoder) -> Unit = {
            it.encodeMap({ it ->
                encodeRequirements(it)
                if (!schemaMap.isEmpty()) {
                    it.addMap("entry", { entries ->
                        schemaMap.entries.forEach({ entry ->
                            entries.add(entry.key, entry.value)
                        })
                    })
                }
                val realSchemaDefault = schemaDefault
                if (realSchemaDefault != null) {
                    it.add("default", realSchemaDefault)
                }
            })
        }

        override fun verify(entity: String, value: MediaDecoder?): Collection<MediaViolation> {
            val violations = super.verify(entity, value)
            if (violations.size > 0) {
                return violations
            }
            val map = value!!.toMap()
            return map.map { entry ->
                val entryEntity = "$entity.${entry.key}"
                val schema = schemaMap.getOrElse(entry.key, { schemaDefault })
                if (schema == null) {
                    listOf(MediaViolation(entryEntity, MediaRequirement("expected", "true")))
                } else {
                    schema.verify(entryEntity, entry.value)
                }
            }.flatten()
        }

        override fun verify(value: MediaDecoder): MediaReport {
            return MediaReport(verify("", value))
        }
    }

    /**
     * [MediaDecoder] schema, representing no particular type.
     */
    class TypeAny : TypeEntity<TypeAny>() {
        override val type: MediaDecoder.Type
            get () = MediaDecoder.Type.UNDEFINED

        override val self: TypeAny
            get () = this
    }

    companion object {
        private val NULL = TypeNull()
        private val ANY = TypeAny()

        /** Creates new [MediaSchema] verifying null values.  */
        fun typeNull(): TypeNull = NULL

        /** Creates new [MediaSchema] verifying booleans.  */
        fun typeBoolean(): TypeBoolean = TypeBoolean()

        /** Creates new [MediaSchema] verifying numbers.  */
        fun typeNumber(): TypeNumber = TypeNumber()

        /** Creates new [MediaSchema] verifying strings.  */
        fun typeText(): TypeText = TypeText()

        fun typeBlob(): TypeBlob = TypeBlob()

        /** Creates new [MediaSchema] verifying lists.  */
        fun typeList(): TypeList = TypeList()

        /** Creates new [MediaSchema] verifying maps.  */
        fun typeMap(): TypeMap = TypeMap()

        /** Creates new [MediaSchema] verifying any values.  */
        fun typeAny(): TypeAny = ANY
    }
}