# Brilho por Gestos

Aplicativo Android em Kotlin para ajustar o brilho com gestos.

## Como usar
1. Instale e abra o aplicativo.
2. Conceda a permissão de sobreposição e a permissão para modificar configurações do sistema.
3. Toque em **Ativar Brilho por Gestos**.
4. Dê dois toques rápidos na faixa invisível na parte inferior da tela.
5. Após o segundo toque, deslize para cima para aumentar o brilho ou para baixo para diminuir.
6. Solte o dedo para encerrar o ajuste.

## Compilar
Requer JDK 17 e Android SDK 35:
`./gradlew assembleDebug`

O APK de depuração será gerado em `app/build/outputs/apk/debug/app-debug.apk`.

## Observações
- O projeto ainda precisa ser compilado e testado em aparelho real.
- Durante o gesto, uma camada transparente ocupa a tela e captura o toque; por isso, o toque usado para ajustar o brilho não chega ao aplicativo que está por baixo.
- O Android e alguns fabricantes podem limitar serviços de sobreposição em segundo plano. Se o sistema encerrar o serviço, abra o app e ative novamente.
