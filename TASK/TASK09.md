
```json
{
  "fileName": "departmentdata.xls",
  "sheetName": "Sheet1",
  "fileSize": 29696,
  "raw": [
    [
      "Department",
      "IT",
      null,
      null,
      null,
      null,
      null,
      "$[0]testName"
    ],
    [
      "Chief"
    ],
    [
      "Name",
      "Age",
      "Birth date",
      "Payment",
      "Bonus",
      "Total",
      null
    ],
    [
      "Maxim",
      30,
      219859200000,
      3000,
      0.25,
      3750
    ],
    [
      null,
      null,
      null,
      null,
      null,
      null,
      null
    ],
    [
      "Employees"
    ],
    [
      "Name",
      "Age",
      "Birth Date",
      "Payment",
      "Bonus",
      "Total",
      "Superior Name"
    ],
    [
      "Oleg",
      32,
      126288000000,
      2000,
      0.2,
      2400,
      "Maxim"
    ],
    [
      "Yuri",
      29,
      244051200000,
      1800,
      0.15,
      2070,
      "Oleg"
    ],
    [
      "Leonid",
      30,
      192902400000,
      1700,
      0.2,
      2040,
      "Oleg"
    ],
    [
      "Alex",
      28,
      272217600000,
      1600,
      0.2,
      1920,
      "Oleg"
    ],
    [
      "Employee Payment Totals:",
      null,
      null,
      7100,
      null,
      8430,
      null
    ],
    [],
    [
      "Total payment:",
      null,
      null,
      10100,
      null,
      12180,
      null
    ]
  ],
  "parsed": {
    "department": {
      "chief.bonus": "Bonus",
      "chief.payment": "Payment",
      "chief.name": "Name",
      "chief.age": "Age"
    },
    "staff": [
      {
        "bonus": 0.2,
        "name": "Oleg",
        "payment": 2000,
        "age": 32
      },
      {
        "bonus": 0.15,
        "name": "Yuri",
        "payment": 1800,
        "age": 29
      },
      {
        "bonus": 0.2,
        "name": "Leonid",
        "payment": 1700,
        "age": 30
      },
      {
        "bonus": 0.2,
        "name": "Alex",
        "payment": 1600,
        "age": 28
      }
    ]
  },
  "totalRows": 14
}
```