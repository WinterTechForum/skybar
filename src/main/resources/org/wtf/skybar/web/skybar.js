angular.module('skybar', [])
  .controller('SkybarController', ['$scope', '$interval', '$http', function ($scope, $interval, $http) {
    $scope.coverage = {
      "placeholder.java": {}
    }

    $scope.sourceFiles = function () {
      var sourceFiles = []
      for (var sourceFile in $scope.coverage) {
        sourceFiles.push(sourceFile)
        console.log(sourceFile)
      }
      return sourceFiles
    }

    $interval(
      (function () {
        $http.get(
          '/coverage.json'
        ).success(function (data) {
            console.log("data: " + JSON.stringify(data));
            $scope.coverage = data
          }).error(function (data, status) {
            console.log("error loading intermediate stats:");
            console.log("status: " + status);
            console.log("data: " + data);
          })
      }),
      2000
    );
  }]);