@registro
Feature: Registro de usuario en la App Mi Banco
  Como cliente activo de Mibanco
  Quiero registrarme en la app con mi documento
  Para acceder a los servicios digitales

  # Los datos de cada iteracion (documento, clave, OTP) vienen del archivo de control.

  @38498 @CC @HappyPath
  Scenario: Registro exitoso con cédula de ciudadanía de un cliente no registrado
    Given que un cliente activo no registrado está en la bienvenida de la app
    When inicia su registro con su tipo y número de documento
    And valida su identidad con el código OTP
    And crea una clave válida aceptando los términos y condiciones
    Then ve la confirmación de registro exitoso
