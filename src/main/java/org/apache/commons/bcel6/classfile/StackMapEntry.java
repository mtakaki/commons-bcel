/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.commons.bcel6.classfile;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.commons.bcel6.Const;

/**
 * This class represents a stack map entry recording the types of
 * local variables and the the of stack items at a given byte code offset.
 * See CLDC specification �5.3.1.2
 *
 * @version $Id$
 * @see     StackMap
 * @see     StackMapType
 */
public final class StackMapEntry implements Node, Cloneable
{

    private int frame_type;
    private int byte_code_offset;
    private StackMapType[] types_of_locals;
    private StackMapType[] types_of_stack_items;
    private ConstantPool constant_pool;


    /**
     * Construct object from input stream.
     *
     * @param input Input stream
     * @throws IOException
     */
    StackMapEntry(final DataInput input, final ConstantPool constant_pool) throws IOException {
        this(input.readByte() & 0xFF, -1, null, null, constant_pool);

        if (this.frame_type >= Const.SAME_FRAME && this.frame_type <= Const.SAME_FRAME_MAX) {
            this.byte_code_offset = this.frame_type - Const.SAME_FRAME;
        } else if (this.frame_type >= Const.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                   this.frame_type <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
            this.byte_code_offset = this.frame_type - Const.SAME_LOCALS_1_STACK_ITEM_FRAME;
            this.types_of_stack_items = new StackMapType[1];
            this.types_of_stack_items[0] = new StackMapType(input, constant_pool);
        } else if (this.frame_type == Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
            this.byte_code_offset = input.readShort();
            this.types_of_stack_items = new StackMapType[1];
            this.types_of_stack_items[0] = new StackMapType(input, constant_pool);
        } else if (this.frame_type >= Const.CHOP_FRAME && this.frame_type <= Const.CHOP_FRAME_MAX) {
            this.byte_code_offset = input.readShort();
        } else if (this.frame_type == Const.SAME_FRAME_EXTENDED) {
            this.byte_code_offset = input.readShort();
        } else if (this.frame_type >= Const.APPEND_FRAME && this.frame_type <= Const.APPEND_FRAME_MAX) {
            this.byte_code_offset = input.readShort();
            final int number_of_locals = this.frame_type - 251;
            this.types_of_locals = new StackMapType[number_of_locals];
            for (int i = 0; i < number_of_locals; i++) {
                this.types_of_locals[i] = new StackMapType(input, constant_pool);
            }
        } else if (this.frame_type == Const.FULL_FRAME) {
            this.byte_code_offset = input.readShort();
            final int number_of_locals = input.readShort();
            this.types_of_locals = new StackMapType[number_of_locals];
            for (int i = 0; i < number_of_locals; i++) {
                this.types_of_locals[i] = new StackMapType(input, constant_pool);
            }
            final int number_of_stack_items = input.readShort();
            this.types_of_stack_items = new StackMapType[number_of_stack_items];
            for (int i = 0; i < number_of_stack_items; i++) {
                this.types_of_stack_items[i] = new StackMapType(input, constant_pool);
            }
        } else {
            /* Can't happen */
            throw new ClassFormatException ("Invalid frame type found while parsing stack map table: " + this.frame_type);
        }
    }

    /**
     * Create an instance
     *
     * @param tag the frame_type to use
     * @param byte_code_offset
     * @param types_of_locals array of {@link StackMapType}s of locals
     * @param types_of_stack_items array ot {@link StackMapType}s of stack items
     * @param constant_pool the constant pool
     */
    public StackMapEntry(final int tag, final int byte_code_offset,
            final StackMapType[] types_of_locals,
            final StackMapType[] types_of_stack_items, final ConstantPool constant_pool) {
        this.frame_type = tag;
        this.byte_code_offset = byte_code_offset;
        this.types_of_locals = types_of_locals != null ? types_of_locals : new StackMapType[0];
        this.types_of_stack_items = types_of_stack_items != null ? types_of_stack_items : new StackMapType[0];
        this.constant_pool = constant_pool;
    }


    /**
     * Dump stack map entry
     *
     * @param file Output file stream
     * @throws IOException
     */
    public final void dump( final DataOutputStream file ) throws IOException {
        file.write(this.frame_type);
        if (this.frame_type >= Const.SAME_FRAME && this.frame_type <= Const.SAME_FRAME_MAX) {
            // nothing to be done
        } else if (this.frame_type >= Const.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                   this.frame_type <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
            this.types_of_stack_items[0].dump(file);
        } else if (this.frame_type == Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
            file.writeShort(this.byte_code_offset);
            this.types_of_stack_items[0].dump(file);
        } else if (this.frame_type >= Const.CHOP_FRAME && this.frame_type <= Const.CHOP_FRAME_MAX) {
            file.writeShort(this.byte_code_offset);
        } else if (this.frame_type == Const.SAME_FRAME_EXTENDED) {
            file.writeShort(this.byte_code_offset);
        } else if (this.frame_type >= Const.APPEND_FRAME && this.frame_type <= Const.APPEND_FRAME_MAX) {
            file.writeShort(this.byte_code_offset);
            for (final StackMapType type : this.types_of_locals) {
                type.dump(file);
            }
        } else if (this.frame_type == Const.FULL_FRAME) {
            file.writeShort(this.byte_code_offset);
            file.writeShort(this.types_of_locals.length);
            for (final StackMapType type : this.types_of_locals) {
                type.dump(file);
            }
            file.writeShort(this.types_of_stack_items.length);
            for (final StackMapType type : this.types_of_stack_items) {
                type.dump(file);
            }
        } else {
            /* Can't happen */
            throw new ClassFormatException ("Invalid Stack map table tag: " + this.frame_type);
        }
    }


    /**
     * @return String representation.
     */
    @Override
    public final String toString() {
        final StringBuilder buf = new StringBuilder(64);
        buf.append("(");
        if (this.frame_type >= Const.SAME_FRAME && this.frame_type <= Const.SAME_FRAME_MAX) {
            buf.append("SAME");
        } else if (this.frame_type >= Const.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                  this.frame_type <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
            buf.append("SAME_LOCALS_1_STACK");
        } else if (this.frame_type == Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
            buf.append("SAME_LOCALS_1_STACK_EXTENDED");
        } else if (this.frame_type >= Const.CHOP_FRAME && this.frame_type <= Const.CHOP_FRAME_MAX) {
            buf.append("CHOP ").append(String.valueOf(251-this.frame_type));
        } else if (this.frame_type == Const.SAME_FRAME_EXTENDED) {
            buf.append("SAME_EXTENDED");
        } else if (this.frame_type >= Const.APPEND_FRAME && this.frame_type <= Const.APPEND_FRAME_MAX) {
            buf.append("APPEND ").append(String.valueOf(this.frame_type-251));
        } else if (this.frame_type == Const.FULL_FRAME) {
            buf.append("FULL");
        } else {
            buf.append("UNKNOWN (").append(this.frame_type).append(")");
        }
        buf.append(", offset delta=").append(this.byte_code_offset);
        if (this.types_of_locals.length > 0) {
            buf.append(", locals={");
            for (int i = 0; i < this.types_of_locals.length; i++) {
                buf.append(this.types_of_locals[i]);
                if (i < this.types_of_locals.length - 1) {
                    buf.append(", ");
                }
            }
            buf.append("}");
        }
        if (this.types_of_stack_items.length > 0) {
            buf.append(", stack items={");
            for (int i = 0; i < this.types_of_stack_items.length; i++) {
                buf.append(this.types_of_stack_items[i]);
                if (i < this.types_of_stack_items.length - 1) {
                    buf.append(", ");
                }
            }
            buf.append("}");
        }
        buf.append(")");
        return buf.toString();
    }


    /**
     * Calculate stack map entry size
     *
     */
    int getMapEntrySize() {
        if (this.frame_type >= Const.SAME_FRAME && this.frame_type <= Const.SAME_FRAME_MAX) {
            return 1;
        } else if (this.frame_type >= Const.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                   this.frame_type <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
            return 1 + (this.types_of_stack_items[0].hasIndex() ? 3 : 1);
        } else if (this.frame_type == Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) {
            return 3 + (this.types_of_stack_items[0].hasIndex() ? 3 : 1);
        } else if (this.frame_type >= Const.CHOP_FRAME && this.frame_type <= Const.CHOP_FRAME_MAX) {
            return 3;
        } else if (this.frame_type == Const.SAME_FRAME_EXTENDED) {
            return 3;
        } else if (this.frame_type >= Const.APPEND_FRAME && this.frame_type <= Const.APPEND_FRAME_MAX) {
            int len = 3;
            for (int i = 0; i < this.types_of_locals.length; i++) {
                len += this.types_of_locals[i].hasIndex() ? 3 : 1;
            }
            return len;
        } else if (this.frame_type == Const.FULL_FRAME) {
            int len = 7;
            for (int i = 0; i < this.types_of_locals.length; i++) {
                len += this.types_of_locals[i].hasIndex() ? 3 : 1;
            }
            for (int i = 0; i < this.types_of_stack_items.length; i++) {
                len += this.types_of_stack_items[i].hasIndex() ? 3 : 1;
            }
            return len;
        } else {
            throw new RuntimeException("Invalid StackMap frame_type: " + this.frame_type);
        }
    }


    public void setFrameType( final int f ) {
        if (f >= Const.SAME_FRAME && f <= Const.SAME_FRAME_MAX) {
            this.byte_code_offset = f - Const.SAME_FRAME;
        } else if (f >= Const.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                   f <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
            this.byte_code_offset = f - Const.SAME_LOCALS_1_STACK_ITEM_FRAME;
        } else if (f == Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (f >= Const.CHOP_FRAME && f <= Const.CHOP_FRAME_MAX) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (f == Const.SAME_FRAME_EXTENDED) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (f >= Const.APPEND_FRAME && f <= Const.APPEND_FRAME_MAX) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (f == Const.FULL_FRAME) { // CHECKSTYLE IGNORE EmptyBlock
        } else {
            throw new RuntimeException("Invalid StackMap frame_type");
        }
        this.frame_type = f;
    }


    public int getFrameType() {
        return this.frame_type;
    }


    public void setByteCodeOffset( final int new_offset ) {
        if (new_offset < 0 || new_offset > 32767) {
            throw new RuntimeException("Invalid StackMap offset: " + new_offset);
        }

        if (this.frame_type >= Const.SAME_FRAME &&
            this.frame_type <= Const.SAME_FRAME_MAX) {
            if (new_offset > Const.SAME_FRAME_MAX) {
                this.frame_type = Const.SAME_FRAME_EXTENDED;
            } else {
                this.frame_type = new_offset;
            }
        } else if (this.frame_type >= Const.SAME_LOCALS_1_STACK_ITEM_FRAME &&
                   this.frame_type <= Const.SAME_LOCALS_1_STACK_ITEM_FRAME_MAX) {
            if (new_offset > Const.SAME_FRAME_MAX) {
                this.frame_type = Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED;
            } else {
                this.frame_type = Const.SAME_LOCALS_1_STACK_ITEM_FRAME + new_offset;
            }
        } else if (this.frame_type == Const.SAME_LOCALS_1_STACK_ITEM_FRAME_EXTENDED) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (this.frame_type >= Const.CHOP_FRAME &&
                   this.frame_type <= Const.CHOP_FRAME_MAX) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (this.frame_type == Const.SAME_FRAME_EXTENDED) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (this.frame_type >= Const.APPEND_FRAME &&
                   this.frame_type <= Const.APPEND_FRAME_MAX) { // CHECKSTYLE IGNORE EmptyBlock
        } else if (this.frame_type == Const.FULL_FRAME) { // CHECKSTYLE IGNORE EmptyBlock
        } else {
            throw new RuntimeException("Invalid StackMap frame_type: " + this.frame_type);
        }
        this.byte_code_offset = new_offset;
    }


    /**
     * Update the distance (as an offset delta) from this StackMap
     * entry to the next.  Note that this might cause the the
     * frame type to change.  Note also that delta may be negative.
     *
     * @param delta offset delta
     */
    public void updateByteCodeOffset(final int delta) {
        this.setByteCodeOffset(this.byte_code_offset + delta);
    }


    public int getByteCodeOffset() {
        return this.byte_code_offset;
    }


    @java.lang.Deprecated
    public void setNumberOfLocals( final int n ) { // TODO unused
    }


    public int getNumberOfLocals() {
        return this.types_of_locals.length;
    }


    public void setTypesOfLocals( final StackMapType[] types ) {
        this.types_of_locals = types != null ? types : new StackMapType[0];
    }


    public StackMapType[] getTypesOfLocals() {
        return this.types_of_locals;
    }


    @java.lang.Deprecated
    public void setNumberOfStackItems( final int n ) { // TODO unused
    }


    public int getNumberOfStackItems() {
        return this.types_of_stack_items.length;
    }


    public void setTypesOfStackItems( final StackMapType[] types ) {
        this.types_of_stack_items = types != null ? types : new StackMapType[0];
    }


    public StackMapType[] getTypesOfStackItems() {
        return this.types_of_stack_items;
    }


    /**
     * @return deep copy of this object
     */
    public StackMapEntry copy() {
        StackMapEntry e;
        try {
            e = (StackMapEntry) this.clone();
        } catch (final CloneNotSupportedException ex) {
            throw new Error("Clone Not Supported");
        }

        e.types_of_locals = new StackMapType[this.types_of_locals.length];
        for (int i = 0; i < this.types_of_locals.length; i++) {
            e.types_of_locals[i] = this.types_of_locals[i].copy();
        }
        e.types_of_stack_items = new StackMapType[this.types_of_stack_items.length];
        for (int i = 0; i < this.types_of_stack_items.length; i++) {
            e.types_of_stack_items[i] = this.types_of_stack_items[i].copy();
        }
        return e;
    }


    /**
     * Called by objects that are traversing the nodes of the tree implicitely
     * defined by the contents of a Java class. I.e., the hierarchy of methods,
     * fields, attributes, etc. spawns a tree of objects.
     *
     * @param v Visitor object
     */
    @Override
    public void accept( final Visitor v ) {
        v.visitStackMapEntry(this);
    }


    /**
     * @return Constant pool used by this object.
     */
    public final ConstantPool getConstantPool() {
        return this.constant_pool;
    }


    /**
     * @param constant_pool Constant pool to be used for this object.
     */
    public final void setConstantPool( final ConstantPool constant_pool ) {
        this.constant_pool = constant_pool;
    }
}
