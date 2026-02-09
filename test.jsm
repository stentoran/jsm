// test loading value to register
ldr a 10
ast a 10

# test loading register contents to register
ldr b 20
ldr a b
ast a 20

// variable test
var: 5
mov b var
ast b 5
print var

// test addition
ldr a 5
ldr b 10
add a b
ast a 15
inc a
ast a 16

// test subtraction
ldr a 28
sub a 16
ast a 12
dec a
ast a 11

// test nand
ldr a 15
ldr b 10
nand a b
ast a -11

// test storing in memory and retrieving from memory
ldr a 5
ldr b 1
// can't do store [1] a
store [b] a
ldr a [1]
ast a 5

ldr b 23
ldr a 12
store [b] a
ast [b] 12
ldr a [b]
ast a 12

// test transfering a value to the stack then popping it to a
push 76
pop a
ast a 76

// test multiplication
ldr a 200
ldr b 10
call mult
ast a 2000

// test bitwise or
ldr a 3
ldr b 5
call or
ast a 7

// how to do a for loop
ldr a 0
bloop 3
add a 1
print a
eloop
ast a 3

// test double for loop
ldr a 3
ldr b 8
call leetcode1

print memory

end

print this should never print

// functions - should go after end so they don't execute unless called

// this is just addition in a for loop
:mult
ldr c a
dec b // will run 1 too many times without this
bloop b
add a c
// print a b c loop
eloop
ret

// bitwise or from bitwise nands
:or
nand a a
nand b b
nand a b
ret

//nested for loop
:leetcode1
bloop b
print "outer loop"
ldr c a
:inner
sub c 1
cmp c z
print "inner loop"
jne inner c z
eloop
ret
