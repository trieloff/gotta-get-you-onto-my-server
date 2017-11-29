var $ = require('shelljs');

$.ls('*.zip').forEach(file => {
  const name = file.replace(/\.zip$/, "");
  $.exec("wsk --apihost " + process.argv[2] + " --auth " + process.argv[3] + " action update " + name + " " + file + " --kind nodejs:6 --web true");
});