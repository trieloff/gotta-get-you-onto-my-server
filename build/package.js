var $ = require('shelljs');
// for js in '*.js'; do; npm ls --prod --parseable | grep node_modules | sed -e 's/.*node_modules/node_modules/' | xargs zip ${js%.*}.zip -r ${js%.*}.js package.json; done

var deps = $.exec("npm ls --prod --parseable", {silent: true})
  .stdout.split("\n")
  .filter(dep => {
    return dep.indexOf("/node_modules/") > 0;
  })
  .map(dep => {
    return dep.replace(/.*node_modules/,"node_modules");
  });


deps.push("main.js");
deps.push("package.json");

$.rm("-f", "main.js");
$.ls('*.js').forEach(file => {
  var zipfile = file.replace(/\.js$/, ".zip");
  var command = deps.slice();
  command.unshift(zipfile);
  command.unshift("bestzip")

  $.echo(file + " " + zipfile);
  $.cp(file, "main.js");
  $.exec(command.join(" "));
});

$.rm("-f", "main.js");