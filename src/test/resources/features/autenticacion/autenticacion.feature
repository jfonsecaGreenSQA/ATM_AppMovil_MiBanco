@autenticacion
Feature: Autenticación y registro en la App Mi Banco
  Como cliente activo de Mibanco
  Quiero registrarme e ingresar a la app con mi documento
  Para acceder a los servicios digitales

  # Los datos de cada iteracion (documento, clave, OTP) vienen del archivo de control.

  # CC (38498) y CE (38502): el tipo de documento viene del archivo de control
  @registro @38498 @38502 @HappyPath
  Scenario: Registro exitoso de un cliente no registrado
    Given que un cliente activo no registrado está en la bienvenida de la app
    When inicia su registro con su tipo y número de documento
    And valida su identidad con el código OTP
    And crea una clave válida aceptando los términos y condiciones
    Then ve la confirmación de registro exitoso

  # CC (38499) y CE (38503). Requiere el dispositivo fisico con la SIM del celular registrado del cliente
  # (LINEA_SMS = SI en el listado).
  @registro @38499 @38503 @SMS
  Scenario: Notificación por SMS de registro exitoso
    Given que un cliente activo no registrado con la línea del dispositivo está en la bienvenida de la app
    When inicia su registro con su tipo y número de documento
    And valida su identidad con el código OTP
    And crea una clave válida aceptando los términos y condiciones
    Then ve la confirmación de registro exitoso
    And recibe en su celular el SMS de registro exitoso

  # CC (38519) y CE (38520): el tipo de documento viene del archivo de control
  @login @38519 @38520 @HappyPath
  Scenario: Login exitoso con documento y clave correcta
    Given que un cliente registrado está en la bienvenida de la app
    When inicia sesión con su tipo y número de documento y su clave
    Then ingresa al Home sin errores
