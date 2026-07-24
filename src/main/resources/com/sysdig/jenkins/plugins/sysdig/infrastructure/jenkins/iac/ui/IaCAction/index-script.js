(function () {
    function onReady(fn) {
        if (document.readyState !== "loading") {
            fn();
        } else {
            document.addEventListener("DOMContentLoaded", fn);
        }
    }

    onReady(function () {
        if (typeof jQuery === "undefined" || !jQuery.fn || !jQuery.fn.DataTable) {
            return;
        }
        var $ = jQuery;
        var common = {
            pageLength: 25,
            lengthMenu: [10, 25, 50, 100],
            autoWidth: false,
            order: [],
        };

        if ($("#findings_table").length) {
            // Column 0 is Severity; each cell carries a numeric data-order so it sorts by risk, not text.
            var findingsTable = $("#findings_table").DataTable(Object.assign({}, common, { order: [[0, "desc"]] }));

            // Severity filter dropdown: smart substring search on the severity column (column 0).
            // Severity names are distinct (none is a substring of another), so a plain term is exact
            // enough and tolerates the surrounding whitespace in the cell markup.
            $("#severity-filter").on("change", function () {
                var sev = $(this).val();
                findingsTable.column(0).search(sev ? String(sev) : "").draw();
            });
        }
        if ($("#unsupported_table").length) {
            $("#unsupported_table").DataTable(common);
        }
        if ($("#errors_table").length) {
            $("#errors_table").DataTable(common);
        }
    });
})();
