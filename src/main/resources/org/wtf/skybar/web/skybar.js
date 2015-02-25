angular.module('skybar', [])
  .controller('SkybarController', ['$scope', '$interval', '$http', '$window', function ($scope, $interval, $http, $window) {
    $scope.sourceFiles = function () {
      var sourceFiles = [];
      for (var sourceFile in $scope.coverage) {
        sourceFiles.push(sourceFile);
        console.log(sourceFile);
      }
      return sourceFiles;
    };

    function updateCoverage() {
        $http.get(
          '/coverage.json'
        ).success(function (data) {
            console.log("data: " + JSON.stringify(data));
            $scope.coverage = data
          }).error(function (data, status) {
            console.log("error loading coverage data:");
            console.log("status: " + status);
            console.log("data: " + data);
            $window.alert("Error loading coverage data.")
          })
      }
    updateCoverage();
    $interval(updateCoverage, 2000);
  }]);