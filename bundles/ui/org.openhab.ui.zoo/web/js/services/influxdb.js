// Generated by CoffeeScript 1.9.2
'use strict';
angular.module('ZooLib.services.influxDb', []).factory('influxDb', function($resource) {
  var host;
  host = '/zoo/influxproxy';
  return $resource(host + '/db/:dbName/series?q=:query', {
    dbName: 'openhab'
  });
});
