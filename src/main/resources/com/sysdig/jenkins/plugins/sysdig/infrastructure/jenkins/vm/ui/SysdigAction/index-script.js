document.addEventListener('DOMContentLoaded', function () {
    const dataHolderDiv = document.getElementById('index-data-holder');
    if (!dataHolderDiv) {
      return;
    }

    const dataGatesSummaryTable = dataHolderDiv.getAttribute('data-gates-summary-table');
    const dataGatesTable = dataHolderDiv.getAttribute('data-gates-table');
    const dataSecurityTable = dataHolderDiv.getAttribute('data-security-table');

    if (dataGatesSummaryTable != '') {
      buildPolicyEvalSummaryTable("#gates_summary_table", JSON.parse(dataGatesSummaryTable));
    }

    if (dataGatesTable != '') {
      buildPolicyEvalTable("#gates_table", dataGatesTable);
    }

    if (dataSecurityTable != '') {
      buildSecurityTable("#security_table", dataSecurityTable);
    }
});
