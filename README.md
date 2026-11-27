# Sistema de Contas a Pagar

Projeto A3 da unidade curricular Algoritmos e Programação da UNISUL.

## Execução

O projeto usa Java 25 e uma interface em Swing (`JOptionPane`). No NetBeans,
abra a pasta do projeto, selecione o JDK 25 e use **Executar Projeto**. A classe
principal é `Main`.

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

As contas ficam em um vetor em memória durante a execução.

O balanço agrupa despesas pelo mês do vencimento. O maior beneficiário é o que
tem a maior soma de valores originais no mês, incluindo contas quitadas no
período selecionado. Empates são resolvidos pela ordem alfabética. Meses sem
despesas aparecem com total zero.

## Roteiro de demonstração

1. Cadastre uma conta de R$ 120,00 com vencimento anterior à data de hoje e
   consulte-a pelo número do documento.
2. Registre uma baixa parcial de R$ 45,00. A confirmação deve mostrar saldo
   inicial de R$ 120,00 e saldo final de R$ 75,00.
3. Abra **Relatórios** e escolha um período que inclua o vencimento. As duas
   listagens devem mostrar valor original de R$ 120,00 e saldo de R$ 75,00.
   No balanço, confira a despesa e o beneficiário no mês do vencimento.
4. Quite os R$ 75,00 restantes. A conta deixa de aparecer nas listagens em
   aberto e vencidas, e o valor original continua no balanço mensal.
