document.addEventListener('DOMContentLoaded', function () {
    // Tab switching logic
    const tabs = document.querySelectorAll('.tabBar .tab a');
    const tabPanes = document.querySelectorAll('.tab-content .tab-pane');

    tabs.forEach(tab => {
        tab.addEventListener('click', function (e) {
            e.preventDefault();

            // Deactivate all tabs and panes
            tabs.forEach(t => t.parentElement.classList.remove('active'));
            tabPanes.forEach(p => {
                p.classList.remove('active');
                p.classList.remove('show');
            });

            // Activate clicked tab and corresponding pane
            const tabPaneId = this.getAttribute('href');
            const activePane = document.querySelector(tabPaneId);
            this.parentElement.classList.add('active');
            if (activePane) {
                activePane.classList.add('active');
                activePane.classList.add('show');
            }
        });
    });


    const dataHolderDiv = document.getElementById('index-data-holder');
    if (!dataHolderDiv) {
      return;
    }

    const dataGatesSummaryTable = dataHolderDiv.getAttribute('data-gates-summary-table');
    const dataGatesTable = dataHolderDiv.getAttribute('data-gates-table');
    const dataSecurityTable = dataHolderDiv.getAttribute('data-security-table');
    const dataDiffTable = dataHolderDiv.getAttribute('data-diff-table');

    if (dataGatesSummaryTable != '') {
      buildPolicyEvalSummaryTable("#gates_summary_table", JSON.parse(dataGatesSummaryTable));
    }

    if (dataGatesTable != '') {
      buildPolicyEvalTable("#gates_table", dataGatesTable);
    }

    if (dataSecurityTable != '') {
      buildSecurityTable("#security_table", dataSecurityTable);
    }

    if (dataDiffTable != '' && dataDiffTable != 'null') {
      buildDiffTable("#diff_added_table", "#diff_fixed_table", dataDiffTable);
    }
});
