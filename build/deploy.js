var $ = require('shelljs');

let code = 0;

$.ls('*.zip').forEach(file => {
  const name = file.replace(/\.zip$/, "");
  $.echo("wsk --apihost " + process.argv[2] + " --auth " + process.argv[3] + " action update " + name + " " + file + " --kind nodejs:6 --web true");
  code += $.exec("wsk --apihost " + process.argv[2] + " --auth " + process.argv[3] + " action update " + name + " " + file + " --kind nodejs:6 --web true").code;
});

$.exit(code);