import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

class JSM{
    public static void main(String[] args) {
        Map<String, Integer> registers = new HashMap<>();
        CPU cpu = new CPU();
        Memory memory = new Memory();
        memory.read(args[0]);
        memory.write();;
        memory.execute();
    }
}

class Memory {
    ArrayList<String> program = new ArrayList<String>();
    Map<String, Integer> labels = new HashMap<>();
    Map<String, Integer> variables = new HashMap<>();
    int[] memory = new int[512];
    int length = 0;

    public void read(String filename) {
        File file = new File(filename);
        try (Scanner reader = new Scanner(file)) {
            while (reader.hasNextLine()) {
                var line = reader.nextLine();
                if (line.isEmpty()) continue;
                if (line.charAt(0) == ';') continue;
                if (line.charAt(0) == '#') continue;
                if (line.substring(0,2).equals("//")) continue;
                String first = line.split("\\s+")[0];
                if (line.charAt(0) == ':') {
                    labels.put(first.substring(1), length); // :label1 something pointless-> label1
                } else if (first.charAt(first.length()-1) == ':') {
                    variables.put(first.substring(0, first.length()-1), Integer.parseInt(line.split("\\s+")[1])); // variable: something
                }
                program.add(line);
                length++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    public void write() {
        try {
            FileWriter writer = new FileWriter("jsm_no_comments.txt");
            for (int i = 0; i<program.size(); i++) {
                writer.write(program.get(i) + '\n');
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("error writing file");
            e.printStackTrace();
        }
    }
    public void execute() {
        CPU cpu = new CPU();
        cpu.labels = labels;
        cpu.variables = variables;
        cpu.programLength = length;
        while (cpu.get("pc") < length) {
            String line = program.get(cpu.get("pc"));
            String[] commands = line.split("\\s+");
            // hardware functions
            if (commands[0].equals("mov")) {
                cpu.mov(commands[1], commands[2]);
            } else if (commands[0].equals("nand")) {
                cpu.nand(commands[1], commands[2]);
            } else if (commands[0].equals("add")) {
                cpu.add(commands[1], commands[2]);
            } else if (commands[0].equals("cmp")) {
                cpu.cmp(commands[1], commands[2]);
            } else if (commands[0].equals("jmpif")) {
                cpu.jmpif(commands[1], commands[2]);
            }
            // microcode functions
            else if (commands[0].equals("ldr")) {
                cpu.ldr(commands[1], commands[2]);
            } else if (commands[0].equals("store")) {
                cpu.store(commands[1], commands[2]);
            } else if (commands[0].equals("jmp")) {
                cpu.jmp(commands[1]);
            } else if (commands[0].equals("jne")) {
                cpu.jne(commands[1], commands[2], commands[3]);
            } else if (commands[0].equals("je")) { // why is this different
                cpu.je(commands[1], commands[2], commands[3]);
            } else if (commands[0].equals("print")) {
                cpu.print(line);
            } else if (commands[0].equals("sub")) {
                cpu.sub(commands[1], commands[2]);
            } else if (commands[0].equals("push")) {
                cpu.push(commands[1]);
            } else if (commands[0].equals("pop")) {
                if (commands.length >= 2) {
                    cpu.pop(commands[1]);
                } else {
                    cpu.pop();
                }
            } else if (commands[0].equals("call")) {
                cpu.call(commands[1]);
            } else if (commands[0].equals("ret")) {
                cpu.ret();
            } else if (commands[0].equals("loop")) {
                cpu.loop(commands[1]);
            } else if (commands[0].equals("bloop")) {
                cpu.bloop(commands[1]);
            } else if (commands[0].equals("eloop")) {
                cpu.eloop();
            } else if (commands[0].equals("not")) {
                cpu.not(commands[1]);
            } else if (commands[0].equals("ast")) {
                cpu.ast(commands[1], commands[2]);
            } else if (commands[0].equals("inc")) {
                cpu.inc(commands[1]);
            } else if (commands[0].equals("dec")) {
                cpu.dec(commands[1]);
            } else if (commands[0].equals("end")) {
                break;
            }
            cpu.inc("pc");
        }
    }

    public int getMemory(int i) {
        return memory[i];
    }
    public void setMemory(int i, int v) {
        memory[i] = v;
    }
    public int getVariable(String v) {
        return variables.get(v);
    }
    public void setVariable(String variable, int value) {
        variables.put(variable, value);
    }
}

class CPU {
    Map<String, Integer> registers = new HashMap<>();
    Map<String, Integer> labels = new HashMap<>();
    Map<String, Integer> variables = new HashMap<>();
    int memLength = 512;
    int[] memory = new int[memLength];
    int programLength = 0;
    CPU() {
        // normal registers
        registers.put("a", 0);
        registers.put("b", 0);
        registers.put("c", 0);
        registers.put("d", 0);
        // zero regiser - always 0, cannot be written to
        registers.put("z", 0);
        // register for microcode functions to not mess up user registers
        registers.put("n", 0);
        // special register
        registers.put("pc", 0); // program counter
        registers.put("rc", -1); // return counter
        registers.put("loop", -1); // loop counter, holds number of times to loop
        registers.put("lj", -1); // holds line to jump to for bloop and loop functions
        // use an int as a boolean array, | and & to set and check flags respectively
        // zero, sign, truth, greater, less, equal
        // 32 16 8 4 2 1
        registers.put("flags", 0);
        registers.put("sp", memLength-1);
    }

    // Begin internal functions
    public boolean isNumber(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public boolean isRegister(String s) {
        return registers.containsKey(s);
    }
    public boolean isAddress(String s) {
        if (s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']') {
            return true;
        }
        return false;
    }
    public int getAddress2(String address) {
        String noBrackets = address.substring(1, address.length() - 1);
        if (isNumber(noBrackets)) {
            return Integer.parseInt(noBrackets);
        }
        if (isRegister(noBrackets)) {
            return get(noBrackets);
        }
        error("could not find address: " + address);
        return -1;
    }
    public boolean isAddressInRegister(String s) {
        if (s.charAt(0) == '[' && s.charAt(s.length() - 1) == ']') {
            return isRegister(s.substring(1, s.length() - 1));
        }
        return false;
    }
    public int getAddress(String address) {
        if (address.charAt(0) == '[') {
            return get(address.substring(1, address.length() - 1));
        }
        return Integer.parseInt(address);
    }
    public void error(String msg) {
        System.out.println("Error at line " + get("pc") + ": " + msg);
    }
    public int getJumpPoint(String label) {
        int val = getValue(label);
        if (val < -1 || val > programLength - 1) {
            error("invalid jump point - label: " + label + " attempt to evaluate: " + val);
            return programLength - 1; // end program
        }
        return getValue(label);
    }
    public int get(String register) {
        try {
            return registers.get(register);
        } catch (NullPointerException e) {
            error(register + " no register: " + register);
            return -1;
        }
    }
    public void set(String register, int value) {
        if (register.equals("z")) {
            error("z register always equals 0 and cannot be set");
            return;
        }
        registers.put(register, value);
    }
    public void setMemory(int index, int value) {
        if (index >= memory.length) return;
        if (index < 0) return;
        memory[index] = value;
    }
    public int getMemory(int index) {
        if (index >= memory.length) return -1;
        if (index < 0) return -1;
        return memory[index];
    }
    public boolean isLabel(String label) {
        return labels.containsKey(label);
    }
    public int getLabel(String label) {
        if (isLabel(label)) {
            return labels.get(label);
        }
        return -1;
    }
    public boolean isVariable(String variable) {
        return variables.containsKey(variable);
    }
    public int getVariable(String variable) {
        if (isVariable(variable)) {
            return variables.get(variable);
        }
        return -1;
    }
    public int getValue(String unknown) {
        if (isNumber(unknown)) {
            return Integer.parseInt(unknown);
        }
        if (isRegister(unknown)) {
            return get(unknown);
        }
        if (isVariable(unknown)) {
            return getVariable(unknown);
        }
        if (isLabel(unknown)) {
            return getLabel(unknown);
        }
        String noBrackets = unknown.substring(1, unknown.length() - 1);
        if (isNumber(noBrackets)) {
            return getMemory(Integer.parseInt(noBrackets));
        }
        if (isRegister(noBrackets)) {
            return getMemory(get(noBrackets));
        }
        return -1;
    }
    // End internal functions

    public void print(String line) {
        String[] commands = line.split("\\s+");
        String[] items = Arrays.copyOfRange(commands, 1, commands.length);
        for (String s : items) {
            if (s.equals("endl|")) {
                return;
            } else if (s.charAt(0) == '|') {
                System.out.print(s.substring(1));
            } else if (isRegister(s)) {
                System.out.print(get(s));
            } else if (isVariable(s)) {
                System.out.print(getVariable(s));
            } else if (isLabel(s)) {
                System.out.print(getLabel(s));
            } else if (s.equals("memory")) {
                System.out.print(Arrays.toString(memory));
            } else {
                System.out.print(s);
            }
            System.out.print(' ');
        }
        System.out.print('\n');
    }
    // why not?
    public void ast(String unknown, String expectedValue) {
        int v1 = getValue(unknown);
        int v2 = getValue(expectedValue);
        if (v1 != v2) {
            System.out.println("Assertion failed: " + unknown + " does not equal " + expectedValue + " - found values: " + v1 + " " + v2);
        } else {
            //System.out.println("Passed");
        }
    }

    // Begin core functions
    public void ldr(String register, String address) {
        mov(register, address);
    }
    public void store(String address, String register) {
        mov(address, register);
    }
    public void nand(String r1, String r2) {
        // all we need for functional completeness
        // implement nand using inverse nand logic, nand= ~&
        int result = ~(getValue(r1) & getValue(r2));
        if (isRegister(r1)) {
            set(r1, result);
        }
    }
    public void mov(String a, String b) {
        int v1 = getValue(a);
        int v2 = getValue(b);
        if (isRegister(a)) {
            set(a, v2);
        }
        if (isAddress(a)) {
            setMemory(getAddress(a), v2);
        }
    }

    public void add(String r1, String r2) {
        int result = getValue(r1) + getValue(r2);
        if (isRegister(r1)) {
            set(r1, result);
        }
    }
    public void cmp(String a, String b) {
        // only set flags with cmp because that's the most predictable
        int v1 = getValue(a);
        int v2 = getValue(b);
        if (v1 == v2) {
            set("flags", 1); // set 3rd bit to 1
        } else if (v1 < v2) {
            set("flags", 2); // set 4th bit to 1
        } else if (v1 > v2) {
            set("flags", 4); // set 5th bit to 1
        }
    }
    public void jmpif(String label, String flagBit) {
        // example: jmpif alabel 4
        // example uses 8 bit integers for ease of reading
        // 8 bit 4 = 0000 0101
        // let's say flags = 0000 0101
        // so check the third flag bit using
        // 0000 0100 & 0000 0101 = 0000 0100
        int val = getValue(flagBit);
        if ((get("flags") & val) == val) {
            int line = getJumpPoint(label);
            if (line <= -1) return;
            set("pc", line);
        }
    }
    // End core functions

    // Begin microcode functions
    public void jmp(String label) {
        jmpif(label, "0"); // flags & 0 == 0 always
    }
    public void je(String label, String r1, String r2) {
        cmp(r1, r2);
        jmpif(label, "1");
    }
    public void jne(String label) {
        // jump if either the greater than or less than bit is set
        jmpif(label, "2");
        jmpif(label, "4");
    }
    public void jne(String label, String r1, String r2) {
        // jump if either the greater than or less than bit is set
        cmp(r1, r2);
        jmpif(label, "2");
        jmpif(label, "4");
    }
    public void push(String data) {
        sub("sp", "1");
        store("[sp]", data);
    }
    public void pop(String register) {
        ldr(register, "[sp]");
        add("sp", "1");
    }
    public void pop() {
        pop("n");
    }
    public void not(String register) {
        nand(register, register);
    }
    public void sub(String destination, String source) {
        // a - b = a + (~b + 1)
        ldr("n", source); // preserve source's value
        not("n");
        add("n", "1");
        add(destination, "n");
    }
    public void call(String label) {
        // if this is implemented in code you need to account for the extra line and probably add 1 to the rc or something
        ldr("rc", "pc");
        jmp(label);
    }
    public void ret() {
        jmp("rc");
    }
    public void loop(String label) {
        sub("loop", "1");
        cmp("loop", "z");
        jne(label);
    }
    public void bloop(String iterations) {
        ldr("lj", "pc");
        ldr("loop", iterations);
        sub("loop", "1");
    }
    public void eloop() {
        cmp("loop", "z");
        sub("loop", "1");
        jne("lj");
    }
    public void inc(String r) {
        add(r, "1");
    }
    public void dec(String r) {
        sub(r, "1");
    }
}
