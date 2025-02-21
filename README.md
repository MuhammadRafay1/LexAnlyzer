# Lexical Analyzer for Custom Language

Welcome to the **Lexical Analyzer** for a custom programming language. This project builds a **lexical analyzer** that converts source code into tokens using **finite automata** (NFA & DFA). It supports various **data types, operators, keywords, and comments** with well-defined **regular expressions**.

## 🚀 Features
- **Custom Tokenization**: Converts source code into categorized tokens.
- **Finite Automata Implementation**: Uses **NFA** (Thompson's construction) and **DFA** for efficient lexical analysis.
- **Symbol Table Management**: Stores identifiers with scopes.
- **Comment Handling**: Ignores **single-line** and **multi-line** comments.
- **Operator Recognition**: Supports arithmetic and exponentiation.

---

## 📝 Language Rules
### **Reserved Keywords**
| Type      | Keyword   |
|-----------|----------|
| Integer   | `int`    |
| Decimal   | `dec`    |
| Boolean   | `bln`    |
| Character | `char`   |
| Global    | `mrWorld`|
| Local     | `mrArea` |

### **Data Types**
- **Integer** (`int`)
- **Decimal** (`dec`)
- **Boolean** (`bln`)
- **Character** (`char`)

### **Constraints**
- **Only lowercase alphabets** allowed for identifiers `[a-z]`.

### **Comments**
| Type       | Syntax            |
|------------|------------------|
| Single-Line | `%% This is a comment` |
| Multi-Line  | `%* This is a
multi-line comment *%` |

### **Operators**
| Operator | Description          |
|----------|----------------------|
| `+`      | Addition             |
| `-`      | Subtraction          |
| `*`      | Multiplication       |
| `/`      | Division             |
| `%`      | Modulus              |
| `^`      | Exponentiation       |

---

## 🔍 Regular Expressions
### **Token Patterns**
| Token Type | Regex |
|------------|-------|
| **Reserved Keywords** | `(int|dec|bln|char|mrWorld|mrArea)` |
| **Identifiers** | `[a-z][a-z0-9_]*` |
| **Integer** | `\d+` |
| **Decimal** | `\d+\.\d{1,5}` |
| **Boolean** | `(true|false)` |
| **Character** | `'[a-z]'` |
| **Single-line Comment** | `%%.*` |
| **Multi-line Comment** | `%\*(*(?!%)|[^*])*\*%` |
| **Operators** | `[+\-*/%^]` |
| **Whitespace** | `[ \t\n]+` |

---

## 🛠 How It Works
1. **Define Tokens** using **Regular Expressions**.
2. **Convert Regular Expressions to NFA** using **Thompson’s construction**.
3. **Transform NFA to DFA** for faster token recognition.
4. **Scan Input Source Code**, match tokens, and generate **a symbol table**.
5. **Skip Comments** and **Report Errors** for invalid syntax.

---

## 📌 Example Code & Token Output
### **Sample Input (`test.sui`)**
```sui
int x = 10
dec y = 3.14
mrWorld z = true
%* This is a multi-line comment *%
x + y ^ 2
```

### **Tokenized Output**
```
Token [type=KEYWORD, value=int, line=1]
Token [type=IDENTIFIER, value=x, line=1]
Token [type=ASSIGNMENT, value==, line=1]
Token [type=INTEGER, value=10, line=1]
...
Token [type=OPERATOR, value=^, line=5]
```

---

## 🛠 Installation & Usage
### **1️⃣ Clone the Repository**
```sh
git clone https://github.com/MuhammadRafay1/LexAnlyzer.git
cd LexAnlyzer
```

### **2️⃣ Compile & Run**
```sh
javac src/*.java -d bin/
java -cp bin/ Main
```

### **3️⃣ Modify `test.sui` and Re-run**
Update your test file and analyze different inputs.


