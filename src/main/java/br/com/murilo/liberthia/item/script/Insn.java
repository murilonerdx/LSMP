package br.com.murilo.liberthia.item.script;

/**
 * Single bytecode instruction for the LiberScript VM. Tagged-union style: a
 * single {@link Op} plus an optional payload whose meaning depends on the op.
 */
public final class Insn {
    public enum Op {
        PUSH,        // payload: Number / String / Boolean
        LOAD,        // payload: String (var name)
        STORE,       // payload: String (var name)
        ADD, SUB, MUL, DIV, MOD,
        EQ, NEQ, LT, LE, GT, GE,
        AND, OR, NOT, NEG,
        JMP,         // payload: Integer (target pc)
        JFALSE,      // payload: Integer (target pc)
        RUN,         // payload: String (template, {var} interpolated)
        SAY,         // payload: String (template)
        WAIT,        // pops ticks
        HALT
    }

    public final Op op;
    public final Object payload;
    /** mutable so the compiler can patch jump targets. */
    public Integer jumpTarget;

    public Insn(Op op, Object payload) {
        this.op = op;
        this.payload = payload;
        if (op == Op.JMP || op == Op.JFALSE) {
            this.jumpTarget = (Integer) payload;
        }
    }

    public Insn(Op op) {
        this(op, null);
    }

    public int target() {
        return jumpTarget == null ? -1 : jumpTarget;
    }

    @Override
    public String toString() {
        return op + (payload == null ? "" : " " + payload);
    }
}
