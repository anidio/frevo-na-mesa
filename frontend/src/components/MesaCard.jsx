import React from 'react';

const MesaCard = ({ mesa, onClick }) => {
  // Objeto para mapear o status às cores do Tailwind
  const statusStyles = {
    LIVRE: {
      border: 'border-green-500',
      text: 'text-green-600 dark:text-green-400',
      tagBg: 'bg-green-100 dark:bg-green-900/50',
    },
    OCUPADA: {
      border: 'border-yellow-500',
      text: 'text-yellow-600 dark:text-yellow-400',
      tagBg: 'bg-yellow-100 dark:bg-yellow-900/50',
    },
    PAGA: {
      border: 'border-blue-500',
      text: 'text-blue-600 dark:text-blue-400',
      tagBg: 'bg-blue-100 dark:bg-blue-900/50',
    },
  };

  const config = statusStyles[mesa.status];

  return (
    <div 
      onClick={onClick}
      // Estilo base do card, inspirado na HomePage
      className={`
        bg-white dark:bg-tema-surface-dark 
        rounded-xl shadow-lg border border-gray-200 dark:border-gray-700
        transition-all duration-300 transform hover:scale-105 cursor-pointer
        border-t-4 ${config.border} 
        flex flex-col p-4 min-h-[120px]
      `}
    >
      {/* Corpo do Card */}
      <div className="flex-grow flex flex-col justify-center text-center">
        <p className="text-xl font-bold text-tema-text dark:text-tema-text-dark">
          Mesa {mesa.numero}
        </p>
        
        {/* Informações específicas do status */}
        {mesa.status === 'LIVRE' && (
          <p className={`mt-1 text-sm font-semibold ${config.text}`}>Disponível</p>
        )}

        {mesa.status === 'OCUPADA' && (
          <>
            <p className="text-2xl font-bold text-tema-text dark:text-tema-text-dark mt-1">
              R$ {mesa.valorTotal.toFixed(2).replace('.', ',')}
            </p>
            {mesa.nomeCliente && (
              <p className="text-xs text-tema-text-muted dark:text-tema-text-muted-dark truncate" title={mesa.nomeCliente}>
                {mesa.nomeCliente}
              </p>
            )}
          </>
        )}
        
        {mesa.status === 'PAGA' && (
          <p className={`mt-1 text-sm font-semibold ${config.text}`}>Aguardando liberação</p>
        )}
      </div>

      {/* Tag de Status no rodapé */}
      <div className="flex justify-center mt-2">
        <span className={`px-3 py-0.5 text-xs font-bold rounded-full ${config.tagBg} ${config.text}`}>
          {mesa.status}
        </span>
      </div>
    </div>
  );
};

export default MesaCard;