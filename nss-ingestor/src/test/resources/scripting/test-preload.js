function multiply(a, b) {
    return a * b;
}

let globalCounter = 0;
let wasExecuted = false;

function setCounter(val) {
    globalCounter = val;
}

function getCounter() {
    return globalCounter;
}

function createObject(name, age) {
    return {
        name: name,
        age: age
    };
}
