// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: vin-events.proto

package org.openhab.binding.mercedesme.internal.proto;

public final class VinEvents {
    private VinEvents() {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {
    }

    public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
        registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
    }

    public interface VINUpdateOrBuilder extends
            // @@protoc_insertion_point(interface_extends:proto.VINUpdate)
            com.google.protobuf.MessageOrBuilder {

        /**
         * <code>repeated string addedVINs = 1;</code>
         * 
         * @return A list containing the addedVINs.
         */
        java.util.List<java.lang.String> getAddedVINsList();

        /**
         * <code>repeated string addedVINs = 1;</code>
         * 
         * @return The count of addedVINs.
         */
        int getAddedVINsCount();

        /**
         * <code>repeated string addedVINs = 1;</code>
         * 
         * @param index The index of the element to return.
         * @return The addedVINs at the given index.
         */
        java.lang.String getAddedVINs(int index);

        /**
         * <code>repeated string addedVINs = 1;</code>
         * 
         * @param index The index of the value to return.
         * @return The bytes of the addedVINs at the given index.
         */
        com.google.protobuf.ByteString getAddedVINsBytes(int index);

        /**
         * <code>repeated string deletedVINs = 2;</code>
         * 
         * @return A list containing the deletedVINs.
         */
        java.util.List<java.lang.String> getDeletedVINsList();

        /**
         * <code>repeated string deletedVINs = 2;</code>
         * 
         * @return The count of deletedVINs.
         */
        int getDeletedVINsCount();

        /**
         * <code>repeated string deletedVINs = 2;</code>
         * 
         * @param index The index of the element to return.
         * @return The deletedVINs at the given index.
         */
        java.lang.String getDeletedVINs(int index);

        /**
         * <code>repeated string deletedVINs = 2;</code>
         * 
         * @param index The index of the value to return.
         * @return The bytes of the deletedVINs at the given index.
         */
        com.google.protobuf.ByteString getDeletedVINsBytes(int index);
    }

    /**
     * Protobuf type {@code proto.VINUpdate}
     */
    public static final class VINUpdate extends com.google.protobuf.GeneratedMessageV3 implements
            // @@protoc_insertion_point(message_implements:proto.VINUpdate)
            VINUpdateOrBuilder {
        private static final long serialVersionUID = 0L;

        // Use VINUpdate.newBuilder() to construct.
        private VINUpdate(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
            super(builder);
        }

        private VINUpdate() {
            addedVINs_ = com.google.protobuf.LazyStringArrayList.emptyList();
            deletedVINs_ = com.google.protobuf.LazyStringArrayList.emptyList();
        }

        @java.lang.Override
        @SuppressWarnings({ "unused" })
        protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
            return new VINUpdate();
        }

        public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
            return org.openhab.binding.mercedesme.internal.proto.VinEvents.internal_static_proto_VINUpdate_descriptor;
        }

        @java.lang.Override
        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
            return org.openhab.binding.mercedesme.internal.proto.VinEvents.internal_static_proto_VINUpdate_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(
                            org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate.class,
                            org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate.Builder.class);
        }

        public static final int ADDEDVINS_FIELD_NUMBER = 1;
        @SuppressWarnings("serial")
        private com.google.protobuf.LazyStringArrayList addedVINs_ = com.google.protobuf.LazyStringArrayList
                .emptyList();

        /**
         * <code>repeated string addedVINs = 1;</code>
         * 
         * @return A list containing the addedVINs.
         */
        public com.google.protobuf.ProtocolStringList getAddedVINsList() {
            return addedVINs_;
        }

        /**
         * <code>repeated string addedVINs = 1;</code>
         * 
         * @return The count of addedVINs.
         */
        public int getAddedVINsCount() {
            return addedVINs_.size();
        }

        /**
         * <code>repeated string addedVINs = 1;</code>
         * 
         * @param index The index of the element to return.
         * @return The addedVINs at the given index.
         */
        public java.lang.String getAddedVINs(int index) {
            return addedVINs_.get(index);
        }

        /**
         * <code>repeated string addedVINs = 1;</code>
         * 
         * @param index The index of the value to return.
         * @return The bytes of the addedVINs at the given index.
         */
        public com.google.protobuf.ByteString getAddedVINsBytes(int index) {
            return addedVINs_.getByteString(index);
        }

        public static final int DELETEDVINS_FIELD_NUMBER = 2;
        @SuppressWarnings("serial")
        private com.google.protobuf.LazyStringArrayList deletedVINs_ = com.google.protobuf.LazyStringArrayList
                .emptyList();

        /**
         * <code>repeated string deletedVINs = 2;</code>
         * 
         * @return A list containing the deletedVINs.
         */
        public com.google.protobuf.ProtocolStringList getDeletedVINsList() {
            return deletedVINs_;
        }

        /**
         * <code>repeated string deletedVINs = 2;</code>
         * 
         * @return The count of deletedVINs.
         */
        public int getDeletedVINsCount() {
            return deletedVINs_.size();
        }

        /**
         * <code>repeated string deletedVINs = 2;</code>
         * 
         * @param index The index of the element to return.
         * @return The deletedVINs at the given index.
         */
        public java.lang.String getDeletedVINs(int index) {
            return deletedVINs_.get(index);
        }

        /**
         * <code>repeated string deletedVINs = 2;</code>
         * 
         * @param index The index of the value to return.
         * @return The bytes of the deletedVINs at the given index.
         */
        public com.google.protobuf.ByteString getDeletedVINsBytes(int index) {
            return deletedVINs_.getByteString(index);
        }

        private byte memoizedIsInitialized = -1;

        @java.lang.Override
        public final boolean isInitialized() {
            byte isInitialized = memoizedIsInitialized;
            if (isInitialized == 1)
                return true;
            if (isInitialized == 0)
                return false;

            memoizedIsInitialized = 1;
            return true;
        }

        @java.lang.Override
        public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
            for (int i = 0; i < addedVINs_.size(); i++) {
                com.google.protobuf.GeneratedMessageV3.writeString(output, 1, addedVINs_.getRaw(i));
            }
            for (int i = 0; i < deletedVINs_.size(); i++) {
                com.google.protobuf.GeneratedMessageV3.writeString(output, 2, deletedVINs_.getRaw(i));
            }
            getUnknownFields().writeTo(output);
        }

        @java.lang.Override
        public int getSerializedSize() {
            int size = memoizedSize;
            if (size != -1)
                return size;

            size = 0;
            {
                int dataSize = 0;
                for (int i = 0; i < addedVINs_.size(); i++) {
                    dataSize += computeStringSizeNoTag(addedVINs_.getRaw(i));
                }
                size += dataSize;
                size += 1 * getAddedVINsList().size();
            }
            {
                int dataSize = 0;
                for (int i = 0; i < deletedVINs_.size(); i++) {
                    dataSize += computeStringSizeNoTag(deletedVINs_.getRaw(i));
                }
                size += dataSize;
                size += 1 * getDeletedVINsList().size();
            }
            size += getUnknownFields().getSerializedSize();
            memoizedSize = size;
            return size;
        }

        @java.lang.Override
        public boolean equals(final java.lang.Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate)) {
                return super.equals(obj);
            }
            org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate other = (org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate) obj;

            if (!getAddedVINsList().equals(other.getAddedVINsList()))
                return false;
            if (!getDeletedVINsList().equals(other.getDeletedVINsList()))
                return false;
            if (!getUnknownFields().equals(other.getUnknownFields()))
                return false;
            return true;
        }

        @java.lang.Override
        public int hashCode() {
            if (memoizedHashCode != 0) {
                return memoizedHashCode;
            }
            int hash = 41;
            hash = (19 * hash) + getDescriptor().hashCode();
            if (getAddedVINsCount() > 0) {
                hash = (37 * hash) + ADDEDVINS_FIELD_NUMBER;
                hash = (53 * hash) + getAddedVINsList().hashCode();
            }
            if (getDeletedVINsCount() > 0) {
                hash = (37 * hash) + DELETEDVINS_FIELD_NUMBER;
                hash = (53 * hash) + getDeletedVINsList().hashCode();
            }
            hash = (29 * hash) + getUnknownFields().hashCode();
            memoizedHashCode = hash;
            return hash;
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate parseFrom(
                java.nio.ByteBuffer data) throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate parseFrom(
                java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate parseFrom(
                com.google.protobuf.ByteString data) throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate parseFrom(
                com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate parseFrom(byte[] data)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data);
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate parseFrom(byte[] data,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return PARSER.parseFrom(data, extensionRegistry);
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate parseFrom(
                java.io.InputStream input) throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate parseFrom(
                java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate parseDelimitedFrom(
                java.io.InputStream input) throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate parseDelimitedFrom(
                java.io.InputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
                    extensionRegistry);
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate parseFrom(
                com.google.protobuf.CodedInputStream input) throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate parseFrom(
                com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
        }

        @java.lang.Override
        public Builder newBuilderForType() {
            return newBuilder();
        }

        public static Builder newBuilder() {
            return DEFAULT_INSTANCE.toBuilder();
        }

        public static Builder newBuilder(org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate prototype) {
            return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
        }

        @java.lang.Override
        public Builder toBuilder() {
            return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
        }

        @java.lang.Override
        protected Builder newBuilderForType(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
            Builder builder = new Builder(parent);
            return builder;
        }

        /**
         * Protobuf type {@code proto.VINUpdate}
         */
        public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
                // @@protoc_insertion_point(builder_implements:proto.VINUpdate)
                org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdateOrBuilder {
            public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
                return org.openhab.binding.mercedesme.internal.proto.VinEvents.internal_static_proto_VINUpdate_descriptor;
            }

            @java.lang.Override
            protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
                return org.openhab.binding.mercedesme.internal.proto.VinEvents.internal_static_proto_VINUpdate_fieldAccessorTable
                        .ensureFieldAccessorsInitialized(
                                org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate.class,
                                org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate.Builder.class);
            }

            // Construct using org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate.newBuilder()
            private Builder() {
            }

            private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
                super(parent);
            }

            @java.lang.Override
            public Builder clear() {
                super.clear();
                bitField0_ = 0;
                addedVINs_ = com.google.protobuf.LazyStringArrayList.emptyList();
                deletedVINs_ = com.google.protobuf.LazyStringArrayList.emptyList();
                return this;
            }

            @java.lang.Override
            public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
                return org.openhab.binding.mercedesme.internal.proto.VinEvents.internal_static_proto_VINUpdate_descriptor;
            }

            @java.lang.Override
            public org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate getDefaultInstanceForType() {
                return org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate.getDefaultInstance();
            }

            @java.lang.Override
            public org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate build() {
                org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate result = buildPartial();
                if (!result.isInitialized()) {
                    throw newUninitializedMessageException(result);
                }
                return result;
            }

            @java.lang.Override
            public org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate buildPartial() {
                org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate result = new org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate(
                        this);
                if (bitField0_ != 0) {
                    buildPartial0(result);
                }
                onBuilt();
                return result;
            }

            private void buildPartial0(org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate result) {
                int from_bitField0_ = bitField0_;
                if (((from_bitField0_ & 0x00000001) != 0)) {
                    addedVINs_.makeImmutable();
                    result.addedVINs_ = addedVINs_;
                }
                if (((from_bitField0_ & 0x00000002) != 0)) {
                    deletedVINs_.makeImmutable();
                    result.deletedVINs_ = deletedVINs_;
                }
            }

            @java.lang.Override
            public Builder clone() {
                return super.clone();
            }

            @java.lang.Override
            public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
                return super.setField(field, value);
            }

            @java.lang.Override
            public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
                return super.clearField(field);
            }

            @java.lang.Override
            public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
                return super.clearOneof(oneof);
            }

            @java.lang.Override
            public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, int index,
                    java.lang.Object value) {
                return super.setRepeatedField(field, index, value);
            }

            @java.lang.Override
            public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
                    java.lang.Object value) {
                return super.addRepeatedField(field, value);
            }

            @java.lang.Override
            public Builder mergeFrom(com.google.protobuf.Message other) {
                if (other instanceof org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate) {
                    return mergeFrom((org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate) other);
                } else {
                    super.mergeFrom(other);
                    return this;
                }
            }

            public Builder mergeFrom(org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate other) {
                if (other == org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate.getDefaultInstance())
                    return this;
                if (!other.addedVINs_.isEmpty()) {
                    if (addedVINs_.isEmpty()) {
                        addedVINs_ = other.addedVINs_;
                        bitField0_ |= 0x00000001;
                    } else {
                        ensureAddedVINsIsMutable();
                        addedVINs_.addAll(other.addedVINs_);
                    }
                    onChanged();
                }
                if (!other.deletedVINs_.isEmpty()) {
                    if (deletedVINs_.isEmpty()) {
                        deletedVINs_ = other.deletedVINs_;
                        bitField0_ |= 0x00000002;
                    } else {
                        ensureDeletedVINsIsMutable();
                        deletedVINs_.addAll(other.deletedVINs_);
                    }
                    onChanged();
                }
                this.mergeUnknownFields(other.getUnknownFields());
                onChanged();
                return this;
            }

            @java.lang.Override
            public final boolean isInitialized() {
                return true;
            }

            @java.lang.Override
            public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
                if (extensionRegistry == null) {
                    throw new java.lang.NullPointerException();
                }
                try {
                    boolean done = false;
                    while (!done) {
                        int tag = input.readTag();
                        switch (tag) {
                            case 0:
                                done = true;
                                break;
                            case 10: {
                                java.lang.String s = input.readStringRequireUtf8();
                                ensureAddedVINsIsMutable();
                                addedVINs_.add(s);
                                break;
                            } // case 10
                            case 18: {
                                java.lang.String s = input.readStringRequireUtf8();
                                ensureDeletedVINsIsMutable();
                                deletedVINs_.add(s);
                                break;
                            } // case 18
                            default: {
                                if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                                    done = true; // was an endgroup tag
                                }
                                break;
                            } // default:
                        } // switch (tag)
                    } // while (!done)
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.unwrapIOException();
                } finally {
                    onChanged();
                } // finally
                return this;
            }

            private int bitField0_;

            private com.google.protobuf.LazyStringArrayList addedVINs_ = com.google.protobuf.LazyStringArrayList
                    .emptyList();

            private void ensureAddedVINsIsMutable() {
                if (!addedVINs_.isModifiable()) {
                    addedVINs_ = new com.google.protobuf.LazyStringArrayList(addedVINs_);
                }
                bitField0_ |= 0x00000001;
            }

            /**
             * <code>repeated string addedVINs = 1;</code>
             * 
             * @return A list containing the addedVINs.
             */
            public com.google.protobuf.ProtocolStringList getAddedVINsList() {
                addedVINs_.makeImmutable();
                return addedVINs_;
            }

            /**
             * <code>repeated string addedVINs = 1;</code>
             * 
             * @return The count of addedVINs.
             */
            public int getAddedVINsCount() {
                return addedVINs_.size();
            }

            /**
             * <code>repeated string addedVINs = 1;</code>
             * 
             * @param index The index of the element to return.
             * @return The addedVINs at the given index.
             */
            public java.lang.String getAddedVINs(int index) {
                return addedVINs_.get(index);
            }

            /**
             * <code>repeated string addedVINs = 1;</code>
             * 
             * @param index The index of the value to return.
             * @return The bytes of the addedVINs at the given index.
             */
            public com.google.protobuf.ByteString getAddedVINsBytes(int index) {
                return addedVINs_.getByteString(index);
            }

            /**
             * <code>repeated string addedVINs = 1;</code>
             * 
             * @param index The index to set the value at.
             * @param value The addedVINs to set.
             * @return This builder for chaining.
             */
            public Builder setAddedVINs(int index, java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                ensureAddedVINsIsMutable();
                addedVINs_.set(index, value);
                bitField0_ |= 0x00000001;
                onChanged();
                return this;
            }

            /**
             * <code>repeated string addedVINs = 1;</code>
             * 
             * @param value The addedVINs to add.
             * @return This builder for chaining.
             */
            public Builder addAddedVINs(java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                ensureAddedVINsIsMutable();
                addedVINs_.add(value);
                bitField0_ |= 0x00000001;
                onChanged();
                return this;
            }

            /**
             * <code>repeated string addedVINs = 1;</code>
             * 
             * @param values The addedVINs to add.
             * @return This builder for chaining.
             */
            public Builder addAllAddedVINs(java.lang.Iterable<java.lang.String> values) {
                ensureAddedVINsIsMutable();
                com.google.protobuf.AbstractMessageLite.Builder.addAll(values, addedVINs_);
                bitField0_ |= 0x00000001;
                onChanged();
                return this;
            }

            /**
             * <code>repeated string addedVINs = 1;</code>
             * 
             * @return This builder for chaining.
             */
            public Builder clearAddedVINs() {
                addedVINs_ = com.google.protobuf.LazyStringArrayList.emptyList();
                bitField0_ = (bitField0_ & ~0x00000001);
                ;
                onChanged();
                return this;
            }

            /**
             * <code>repeated string addedVINs = 1;</code>
             * 
             * @param value The bytes of the addedVINs to add.
             * @return This builder for chaining.
             */
            public Builder addAddedVINsBytes(com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                checkByteStringIsUtf8(value);
                ensureAddedVINsIsMutable();
                addedVINs_.add(value);
                bitField0_ |= 0x00000001;
                onChanged();
                return this;
            }

            private com.google.protobuf.LazyStringArrayList deletedVINs_ = com.google.protobuf.LazyStringArrayList
                    .emptyList();

            private void ensureDeletedVINsIsMutable() {
                if (!deletedVINs_.isModifiable()) {
                    deletedVINs_ = new com.google.protobuf.LazyStringArrayList(deletedVINs_);
                }
                bitField0_ |= 0x00000002;
            }

            /**
             * <code>repeated string deletedVINs = 2;</code>
             * 
             * @return A list containing the deletedVINs.
             */
            public com.google.protobuf.ProtocolStringList getDeletedVINsList() {
                deletedVINs_.makeImmutable();
                return deletedVINs_;
            }

            /**
             * <code>repeated string deletedVINs = 2;</code>
             * 
             * @return The count of deletedVINs.
             */
            public int getDeletedVINsCount() {
                return deletedVINs_.size();
            }

            /**
             * <code>repeated string deletedVINs = 2;</code>
             * 
             * @param index The index of the element to return.
             * @return The deletedVINs at the given index.
             */
            public java.lang.String getDeletedVINs(int index) {
                return deletedVINs_.get(index);
            }

            /**
             * <code>repeated string deletedVINs = 2;</code>
             * 
             * @param index The index of the value to return.
             * @return The bytes of the deletedVINs at the given index.
             */
            public com.google.protobuf.ByteString getDeletedVINsBytes(int index) {
                return deletedVINs_.getByteString(index);
            }

            /**
             * <code>repeated string deletedVINs = 2;</code>
             * 
             * @param index The index to set the value at.
             * @param value The deletedVINs to set.
             * @return This builder for chaining.
             */
            public Builder setDeletedVINs(int index, java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                ensureDeletedVINsIsMutable();
                deletedVINs_.set(index, value);
                bitField0_ |= 0x00000002;
                onChanged();
                return this;
            }

            /**
             * <code>repeated string deletedVINs = 2;</code>
             * 
             * @param value The deletedVINs to add.
             * @return This builder for chaining.
             */
            public Builder addDeletedVINs(java.lang.String value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                ensureDeletedVINsIsMutable();
                deletedVINs_.add(value);
                bitField0_ |= 0x00000002;
                onChanged();
                return this;
            }

            /**
             * <code>repeated string deletedVINs = 2;</code>
             * 
             * @param values The deletedVINs to add.
             * @return This builder for chaining.
             */
            public Builder addAllDeletedVINs(java.lang.Iterable<java.lang.String> values) {
                ensureDeletedVINsIsMutable();
                com.google.protobuf.AbstractMessageLite.Builder.addAll(values, deletedVINs_);
                bitField0_ |= 0x00000002;
                onChanged();
                return this;
            }

            /**
             * <code>repeated string deletedVINs = 2;</code>
             * 
             * @return This builder for chaining.
             */
            public Builder clearDeletedVINs() {
                deletedVINs_ = com.google.protobuf.LazyStringArrayList.emptyList();
                bitField0_ = (bitField0_ & ~0x00000002);
                ;
                onChanged();
                return this;
            }

            /**
             * <code>repeated string deletedVINs = 2;</code>
             * 
             * @param value The bytes of the deletedVINs to add.
             * @return This builder for chaining.
             */
            public Builder addDeletedVINsBytes(com.google.protobuf.ByteString value) {
                if (value == null) {
                    throw new NullPointerException();
                }
                checkByteStringIsUtf8(value);
                ensureDeletedVINsIsMutable();
                deletedVINs_.add(value);
                bitField0_ |= 0x00000002;
                onChanged();
                return this;
            }

            @java.lang.Override
            public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
                return super.setUnknownFields(unknownFields);
            }

            @java.lang.Override
            public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
                return super.mergeUnknownFields(unknownFields);
            }

            // @@protoc_insertion_point(builder_scope:proto.VINUpdate)
        }

        // @@protoc_insertion_point(class_scope:proto.VINUpdate)
        private static final org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate DEFAULT_INSTANCE;
        static {
            DEFAULT_INSTANCE = new org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate();
        }

        public static org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate getDefaultInstance() {
            return DEFAULT_INSTANCE;
        }

        private static final com.google.protobuf.Parser<VINUpdate> PARSER = new com.google.protobuf.AbstractParser<VINUpdate>() {
            @java.lang.Override
            public VINUpdate parsePartialFrom(com.google.protobuf.CodedInputStream input,
                    com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                    throws com.google.protobuf.InvalidProtocolBufferException {
                Builder builder = newBuilder();
                try {
                    builder.mergeFrom(input, extensionRegistry);
                } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                    throw e.setUnfinishedMessage(builder.buildPartial());
                } catch (com.google.protobuf.UninitializedMessageException e) {
                    throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
                } catch (java.io.IOException e) {
                    throw new com.google.protobuf.InvalidProtocolBufferException(e)
                            .setUnfinishedMessage(builder.buildPartial());
                }
                return builder.buildPartial();
            }
        };

        public static com.google.protobuf.Parser<VINUpdate> parser() {
            return PARSER;
        }

        @java.lang.Override
        public com.google.protobuf.Parser<VINUpdate> getParserForType() {
            return PARSER;
        }

        @java.lang.Override
        public org.openhab.binding.mercedesme.internal.proto.VinEvents.VINUpdate getDefaultInstanceForType() {
            return DEFAULT_INSTANCE;
        }
    }

    private static final com.google.protobuf.Descriptors.Descriptor internal_static_proto_VINUpdate_descriptor;
    private static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_proto_VINUpdate_fieldAccessorTable;

    public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
        return descriptor;
    }

    private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
    static {
        java.lang.String[] descriptorData = { "\n\020vin-events.proto\022\005proto\"3\n\tVINUpdate\022\021"
                + "\n\taddedVINs\030\001 \003(\t\022\023\n\013deletedVINs\030\002 \003(\tB/"
                + "\n-org.openhab.binding.mercedesme.interna" + "l.protob\006proto3" };
        descriptor = com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData,
                new com.google.protobuf.Descriptors.FileDescriptor[] {});
        internal_static_proto_VINUpdate_descriptor = getDescriptor().getMessageTypes().get(0);
        internal_static_proto_VINUpdate_fieldAccessorTable = new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
                internal_static_proto_VINUpdate_descriptor, new java.lang.String[] { "AddedVINs", "DeletedVINs", });
    }

    // @@protoc_insertion_point(outer_class_scope)
}
