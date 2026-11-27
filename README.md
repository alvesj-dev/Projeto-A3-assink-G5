# Sistema de Contas a Pagar

Projeto A3 da unidade curricular Algoritmos e Programação da UNISUL.

## Execução

O projeto usa Java 25 e uma interface em Swing (`JOptionPane`). No NetBeans,
abra a pasta do projeto, selecione o JDK 25 e execute a classe `Main`.

Para compilar e executar no PowerShell com o JDK no `PATH`:

```powershell
New-Item -ItemType Directory -Force build/classes | Out-Null
$fontes = Get-ChildItem -Path src -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
javac --release 25 -encoding UTF-8 -d build/classes $fontes
java -cp build/classes Main
```

## Funcionalidades

- Cadastro de contas com inclusão, consulta, alteração e exclusão por documento.
- Baixa parcial ou total com confirmação e exibição do saldo antes e depois.
- Relatórios de contas a pagar, contas vencidas e balanço mensal por período.

Cadastre uma conta com vencimento anterior a hoje e faça uma baixa parcial.
Consulte a conta e gere os relatórios para um período que inclua o vencimento.
As contas ficam em um vetor em memória durante a execução.

O balanço agrupa despesas pelo mês do vencimento. O maior beneficiário é o que
tem a maior soma de débitos no mês. Empates são resolvidos pela ordem alfabética.
